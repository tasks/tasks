files = Dir.glob('**/src/main/res/values/*.xml')
files.each do |path|
    file = File.new(path)
    file.read.scan(/<string name="(.*)">/) do |match|
        result = `git grep R.string.#{match[0]} astrid api` + `git grep string\/#{match[0]} astrid api`
        puts "#{path} - #{match}" if result.empty?
    end
    file = File.new(path)
    file.read.scan(/<string-array name="(.*)">/) do |match|
      result = `git grep R.array.#{match[0]} astrid api` + `git grep array\/#{match[0]} astrid api`
      puts "#{path} - #{match}" if result.empty?
    end
end
