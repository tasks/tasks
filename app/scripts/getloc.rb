#!/usr/bin/env ruby

require 'fileutils'

$:.unshift File.dirname(__FILE__)
require 'clean_translations'

# Script for invoking the GetLocalization tools
# IMPORTANT: Right now, must be invoked from the project's root directory.
# Usage: ./bin/getloc.rb [cmd] [lang]
# cmd: 'export' or 'import'

# lang: Language code or 'master'

PROJECT_NAME='tasks_android'
LANGUAGE_MAP = {
    "el" => "grk",
    "sk" => "sk-SK",
    "hu" => "hu-HU",
    "fa" => "pes-IR"
}

# Converts astrid language codes to GetLocalization language codes (which don't use -r)
def astrid_code_to_getloc_code(lang)
    (LANGUAGE_MAP[lang] || lang).sub("-r", "-")
end

# Inverse of the above function
def getloc_code_to_astrid_code(lang)
    (LANGUAGE_MAP.invert[lang] || lang).sub("-", "-r")
end

# Uploads files for the specified language to GetLocalization
# tmp_files (Array): temporary strings files to use
# lang (String): language code
# src_files_block (lambda): Block for computing the source file list from the language code
def export(tmp_files, lang, src_files_block)
  src_files = src_files_block.call(lang)
  for i in 0...tmp_files.length
    %x(cp #{src_files[i]} #{tmp_files[i]}) if src_files[i] != tmp_files[i]
  end

  tmp_files.each do |f|
    %x(gsed -i "s/\\\\\\'/'/g" #{f})
  end

  if lang == "master"
    tmp_files.each do |f|
      puts "Updating master file #{f}"
      %x(curl --form file=@#{f} --user "#{@user}:#{@password}" https://api.getlocalization.com/#{PROJECT_NAME}/api/update-master/)
    end
  else
      raise "dont do this if you already exported your translations"
    lang_tmp = astrid_code_to_getloc_code(lang)
    tmp_files.each do |f|
      puts "Updating language file #{f}"
      name = File.basename(f)
      %x(curl --form file=@#{f} --user "#{@user}:#{@password}" https://api.getlocalization.com/#{PROJECT_NAME}/api/translations/file/#{name}/#{lang_tmp}/)
    end
  end
end

# Downloads and imports files for the specified language
# tmp_files (Array): temporary strings files to use
# lang (String): language code
# dst_files_block (lambda): Block for computing the destination files list from the language code
def import(tmp_files, lang, dst_files_block)
  if lang == "master"
    tmp_dir = File.dirname(tmp_files[0])
    tmp_all = File.join(tmp_dir, "all.zip")
    tmp_all_dir = File.join(tmp_dir, "all")

    %x(curl --user "#{@user}:#{@password}" https://api.getlocalization.com/#{PROJECT_NAME}/api/translations/zip/ -o #{tmp_all})
    %x(mkdir #{tmp_all_dir})
    %x(tar xzf #{tmp_all} -C #{tmp_all_dir})

    # Get all translations
    Dir.foreach(tmp_all_dir) do |l|
      if (l != "." && l != "..")
        lang_local = getloc_code_to_astrid_code(l)
        dst_files = dst_files_block.call(lang_local)

        for i in 0...tmp_files.length
          file = File.join(tmp_all_dir, l, File.basename(tmp_files[i]))
          %x(gsed -i "s/\\([^\\\\\\]\\)'/\\1\\\\\\'/g" #{file})
          puts "Moving #{file} to #{dst_files[i]}"
          %x(mv #{file} #{dst_files[i]})
        end
      end
    end
    %x(rm -rf #{tmp_all_dir})
    %x(rm #{tmp_all})
  else
    lang_tmp = astrid_code_to_getloc_code(lang)
    dst_files = dst_files_block.call(lang)
    for i in 0...tmp_files.length
      name = File.basename(tmp_files[i])
      %x(curl --user "#{@user}:#{@password}" https://api.getlocalization.com/#{PROJECT_NAME}/api/translations/file/#{name}/#{lang_tmp}/ -o #{tmp_files[i]})
      %x(gsed -i "s/\\([^\\\\\\]\\)'/\\1\\\\\\'/g" #{tmp_files[i]})
      `gsed -i '/\s*<!--.*-->\s*$/d' #{tmp_files[i]}` # strip comments
      puts "Moving #{tmp_files[i]} to #{dst_files[i]}"
      %x(mv #{tmp_files[i]} #{dst_files[i]})
    end
    remove_untranslated_strings(*dst_files)
  end

end

class Android
  def self.tmp_files
      FileUtils.mkdir_p "translations"
    ["translations/strings.xml"]
  end

  def self.src_files(cmd, type)
    if cmd == :export && type == "master"
      lambda { |l| ["src/main/res/values/strings.xml"] }
    else
      lambda { |l| ["src/main/res/values-#{l}/strings.xml"] }
    end
  end
end

# Main function for invoking the GetLocalization tools
# cmd (String): Command to invoke. Must be 'import' or 'export'
# lang (String): Language code. Can also be 'master' to specify master files for export or all languages for import.
def getloc(cmd, languages)
  cmd = cmd.to_sym

  raise "must set GETLOC_USER and GETLOC_PASS environment variables" if ENV['GETLOC_USER'].nil? or ENV['GETLOC_PASS'].nil?
  @user = ENV['GETLOC_USER']
  @password = ENV['GETLOC_PASS']
  platform_class = Android
  languages.split(',').each do |lang|
    case cmd
    when :export
      puts "Exporting #{lang} files"
      export(platform_class.tmp_files, lang, platform_class.src_files(cmd, lang))
    when :import
      puts "Importing #{lang} files"
      import(platform_class.tmp_files, lang, platform_class.src_files(cmd, lang))
    else
      puts "Command #{cmd} not recognized. Should be one of 'export' or 'import'."
      return
    end

    platform_class.tmp_files.each do |f|
      %x(rm -f #{f})
    end
  end
end

getloc(*ARGV)
