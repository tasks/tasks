files = Dir.glob('api/src/main/res/values/strings.xml') + Dir.glob('astrid/src/main/res/values/strings-*.xml')
files.each do |path|
    file = File.new(path)
    file.read.scan(/<string name="(.*)">/) do |match|
        result = `git grep R.string.#{match[0]} astrid api` + `git grep string\/#{match[0]} astrid api`
        puts "#{path} - #{match}" if result.empty?
    end
end
