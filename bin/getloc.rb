#!/usr/bin/env ruby
# Script for invoking the GetLocalization tools
# IMPORTANT: Right now, must be invoked from the project's root directory.
# Usage: ./bin/getloc.rb [cmd] [platform] [lang]
# cmd: 'export' or 'import'
# platform: 'android', 'ios', or 'web'
# lang: Language code or 'master'

LangException = Struct.new :platform, :platform_code, :getloc_code

LANG_EXCEPTIONS = [
  LangException.new(:ios, "zh-Hans", "zh-TW"),
  LangException.new(:ios, "zh", "zh-CN")
]


# Converts astrid language codes to GetLocalization language codes (which don't use -r)
def astrid_code_to_getloc_code(lang, platform)
  result = lang.sub("-r", "-")
  exception = LANG_EXCEPTIONS.find { |le| le.platform == platform and le.platform_code == lang }
  exception ? exception.getloc_code : result
end

# Inverse of the above function
def getloc_code_to_astrid_code(lang, platform)
  result = lang.sub("-", "-r")
  exception = LANG_EXCEPTIONS.find { |le| le.platform == platform and le.getloc_code == lang }
  exception ? exception.platform_code : result
end

# Uploads files for the specified language to GetLocalization
# tmp_files (Array): temporary strings files to use
# lang (String): language code
# platform (String): platform; one of 'android', 'ios', 'web'
# src_files_block (lambda): Block for computing the source file list from the language code
def export(tmp_files, lang, platform, src_files_block)
  src_files = src_files_block.call(lang)
  for i in 0...tmp_files.length
    %x(cp #{src_files[i]} #{tmp_files[i]}) if src_files[i] != tmp_files[i]
  end
  
  if platform == :android
    tmp_files.each do |f|
      %x(sed -i '' "s/\\\\\\'/'/g" #{f})
    end
  end

  if lang == "master"
    tmp_files.each do |f|
      puts "Updating master file #{f}"
      %x(curl --form file=@#{f} --user #{@user}:#{@password} https://api.getlocalization.com/astrid/api/update-master/)
    end
  else
    lang_tmp = astrid_code_to_getloc_code(lang, platform)
    tmp_files.each do |f|
      puts "Updating language file #{f}"
      name = File.basename(f)
      %x(curl --form file=@#{f} --user #{@user}:#{@password} https://api.getlocalization.com/astrid/api/translations/file/#{name}/#{lang_tmp}/)
    end
  end
end

# Downloads and imports files for the specified language
# tmp_files (Array): temporary strings files to use
# lang (String): language code
# platform (String): platform; one of 'android', 'ios', or 'web'
# dst_files_block (lambda): Block for computing the destination files list from the language code
def import(tmp_files, lang, platform, dst_files_block)
  if lang == "master"
    tmp_dir = File.dirname(tmp_files[0])
    tmp_all = File.join(tmp_dir, "all.zip")
    tmp_all_dir = File.join(tmp_dir, "all")

    %x(curl --user #{@user}:#{@password} https://api.getlocalization.com/astrid/api/translations/zip/ -o #{tmp_all})
    %x(mkdir #{tmp_all_dir})
    %x(tar xzf #{tmp_all} -C #{tmp_all_dir})

    # Get all translations
    Dir.foreach(tmp_all_dir) do |l|
      if (l != "." && l != "..")
        lang_local = getloc_code_to_astrid_code(l, platform)
        dst_files = dst_files_block.call(lang_local)

        for i in 0...tmp_files.length
          file = File.join(tmp_all_dir, l, File.basename(tmp_files[i]))
          %x(sed -i '' "s/'/\\\\\\'/g" #{file}) if platform == :android
          puts "Moving #{file} to #{dst_files[i]}"
          %x(mv #{file} #{dst_files[i]})
        end
      end
    end
    %x(rm -rf #{tmp_all_dir})
    %x(rm #{tmp_all})
  else
    lang_tmp = astrid_code_to_getloc_code(lang, platform)
    dst_files = dst_files_block.call(lang)
    for i in 0...tmp_files.length
      name = File.basename(tmp_files[i])
      %x(curl --user #{@user}:#{@password} https://api.getlocalization.com/astrid/api/translations/file/#{name}/#{lang_tmp}/ -o #{tmp_files[i]})
      %x(sed -i '' "s/'/\\\\\\'/g" #{tmp_files[i]}) if platform == :android
      puts "Moving #{tmp_files[i]} to #{dst_files[i]}"
      %x(mv #{tmp_files[i]} #{dst_files[i]})
    end
  end

end

class Android
  def self.tmp_files
    ["translations/strings.xml", "translations/strings-api.xml"]
  end

  def self.src_files(cmd, type)
    if cmd == :export && type == "master"
      %x[./bin/catxml astrid/res/values/strings*.xml > #{self.tmp_files[0]}]
      lambda { |l| ["translations/strings.xml", "api/res/values/strings.xml"] }
    else
      lambda { |l| ["astrid/res/values-#{l}/strings.xml", "api/res/values-#{l}/strings.xml"] }
    end
  end
end

class IOS
  def self.tmp_files
    ["Resources/Localizable.strings"]
  end

  def self.src_files(cmd, type)
    if cmd == :export && type == "master"
      lambda { |l| ["Resources/Localizations/en.lproj/Localizable.strings"] }
    else
      lambda { |l| ["Resources/Localizations/#{l}.lproj/Localizable.strings"] }
    end
  end
end

# Main function for invoking the GetLocalization tools
# cmd (String): Command to invoke. Must be 'import' or 'export'
# platform (String): Project platform. Must be 'android', 'ios', or 'web'
# lang (String): Language code. Can also be 'master' to specify master files for export or all languages for import.
def getloc(cmd, platform, lang)
  cmd = cmd.to_sym
  platform = platform.to_sym
  
  @user = "sbosley"
  @password = "ohSed4pe"
  platform_class = nil
  case platform
  when :android
    platform_class = Android 
  when :ios
    platform_class = IOS
  when :web
    puts "Web not yet supported."
    return
  else
    puts "Platform #{platform} not recognized. Should be one of 'android', 'ios', or 'web'."
    return
  end

  case cmd
  when :export
    puts "Exporting #{lang} files"
    export(platform_class.tmp_files, lang, platform, platform_class.src_files(cmd, lang))
  when :import
    puts "Importing #{lang} files"
    import(platform_class.tmp_files, lang, platform, platform_class.src_files(cmd, lang))
  else
    puts "Command #{cmd} not recognized. Should be one of 'export' or 'import'."
    return
  end

  platform_class.tmp_files.each do |f|
    %x(rm -f #{f})
  end
 
end

getloc(*ARGV)
