#!/usr/bin/env ruby

def lang_mod(lang)
  lang.sub("-r", "-")
end

def export(tmp_files, src_files, lang, android)
  for i in 0...tmp_files.length
    %x(cp #{src_files[i]} #{tmp_files[i]}) if src_files[i] != tmp_files[i]
  end
  
  if android
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
    lang_tmp = lang_mod(lang)
    tmp_files.each do |f|
      puts "Updating language file #{f}"
      name = File.basename(f)
      %x(curl --form file=@#{f} --user #{@user}:#{@password} https://api.getlocalization.com/astrid/api/translations/file/#{name}/#{lang_tmp}/)
    end
  end
end



def import(tmp_files, dst_files, lang, android)
  if lang == "master"
    tmp_dir = File.dirname(tmp_files[0])
    tmp_all = File.join(tmp_dir, "all.zip")
    tmp_all_dir = File.join(tmp_dir, "all")

    %x(curl --user #{@user}:#{@password} https://api.getlocalization.com/astrid/api/translations/zip/ -o #{tmp_all})
    %x(mkdir #{tmp_all_dir})
    %x(tar xzf #{tmp_all} -C #{tmp_all_dir})

    # Get all translations
    Dir.foreach(tmp_all_dir) do |f|
      if (f != "." && f != "..")
        for i in 0...tmp_files.length
          file = File.join(tmp_all_dir, f, File.basename(t))
          %x(sed -i '' "s/'/\\\\\\'/g" #{file}) if android
          puts "Moving #{file} to #{dst_files[i]}"
          %x(mv #{file} #{dst_files[i]})
        end
      end
    end
    %x(rm -rf #{tmp_all_dir})
    %x(rm #{tmp_all})
  else
    lang_tmp = lang_mod(lang)
    for i in 0...tmp_files.length
      name = File.basename(tmp_files[i])
      %x(curl --user #{@user}:#{@password} https://api.getlocalization.com/astrid/api/translations/file/#{name}/#{lang_tmp}/ -o #{tmp_files[i]})
      %x(sed -i '' "s/'/\\\\\\'/g" #{tmp_files[i]}) if android
      puts "Moving #{tmp_files[i]} to #{dst_files[i]}"
      %x(mv #{tmp_files[i]} #{dst_files[i]})
    end
  end

end


def getloc(cmd, platform, lang)
  android = false
  @user = "sbosley"
  @password = "ohSed4pe"


  case platform
  when "android"
    android = true
    tmp_files = ["translations/strings.xml", "translations/strings-api.xml"]
    if lang == "master" && cmd == "export"
      %x[./bin/catxml astrid/res/values/strings*.xml > #{tmp_files[0]}]
      src_files = ["translations/strings.xml", "api/res/values/strings.xml"]
    else
      src_files = ["astrid/res/values-#{lang}/strings.xml", "api/res/values-#{lang}/strings.xml"]
    end

  when "ios"
    tmp_files = ["Resources/Localizable.strings"]
    lang_tmp = lang
    lang_tmp = "en" if lang == "master"
    src_files = ["Resources/Localizations/#{lang_tmp}.lproj/Localizable.strings"]

  when "web"
    puts "Web not yet supported."
    return
  else
    puts "Platform #{platform} not recognized. Should be one of 'android', 'ios', or 'web'."
    return
  end

  case cmd
  when "export"
    puts "Exporting #{lang} files"
    export(tmp_files, src_files, lang, android)
  when "import"
    puts "Importing #{lang} files"
    import(tmp_files, src_files, lang, android)
  else
    puts "Command #{cmd} not recognized. Should be one of 'export' or 'import'."
    return
  end

  tmp_files.each do |f|
    %x(rm -f #{f})
  end
 
end

getloc(*ARGV)
