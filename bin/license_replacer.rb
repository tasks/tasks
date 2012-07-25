MATCH = "<!-- See the file \"LICENSE\" for the full license governing this code. -->"

REPLACE = "<!-- 
** Copyright (c) 2012 Todoroo Inc
**
** See the file \"LICENSE\" for the full license governing this code. 
-->"

ARGV.each do |filename|
  contents = File.open(filename, 'rb') { |f| f.read }

  first_line = contents.index("\n")
  if contents[first_line + 1, 4] != "<!--"
    contents = contents[0, first_line + 1] + REPLACE + "\n" + contents[first_line + 1, contents.length]
  end
  File.open(filename, "wb") { |f| f.write(contents) }
end
