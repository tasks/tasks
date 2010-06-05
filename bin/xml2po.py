#!/usr/bin/python -u
# -*- encoding: utf-8 -*-
# Copyright (c) 2004, 2005, 2006 Danilo Šegan <danilo@gnome.org>.
#
# This file is part of xml2po.
#
# xml2po is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# xml2po is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with xml2po; if not, write to the Free Software Foundation, Inc.,
# 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
#

# xml2po -- translate XML documents
VERSION = "1.0.5"

# Versioning system (I use this for a long time, so lets explain it to
# those Linux-versioning-scheme addicts):
#   1.0.* are unstable, development versions
#   1.1 will be first stable release (release 1), and 1.1.* bugfix releases
#   2.0.* will be unstable-feature-development stage (milestone 1)
#   2.1.* unstable development betas (milestone 2)
#   2.2 second stable release (release 2), and 2.2.* bugfix releases
#   ...
#
import sys
import libxml2
import gettext
import os
import re

class NoneTranslations:
    def gettext(self, message):
        return None

    def lgettext(self, message):
        return None

    def ngettext(self, msgid1, msgid2, n):
        return None

    def lngettext(self, msgid1, msgid2, n):
        return None

    def ugettext(self, message):
        return None

    def ungettext(self, msgid1, msgid2, n):
        return None



class MessageOutput:
    def __init__(self, with_translations = 0):
        self.messages = []
        self.comments = {}
        self.linenos = {}
        self.nowrap = {}
        if with_translations:
            self.translations = []
        self.do_translations = with_translations
        self.output_msgstr = 0 # this is msgid mode for outputMessage; 1 is for msgstr mode

    def translationsFollow(self):
        """Indicate that what follows are translations."""
        self.output_msgstr = 1

    def setFilename(self, filename):
        self.filename = filename

    def outputMessage(self, text, lineno = 0, comment = None, spacepreserve = 0, tag = None):
        """Adds a string to the list of messages."""
        if (text.strip() != ''):
            t = escapePoString(normalizeString(text, not spacepreserve))
            if self.output_msgstr:
                self.translations.append(t)
                return
            
            if self.do_translations or (not t in self.messages):
                self.messages.append(t)
                if spacepreserve:
                    self.nowrap[t] = 1
                if t in self.linenos.keys():
                    self.linenos[t].append((self.filename, tag, lineno))
                else:
                    self.linenos[t] = [ (self.filename, tag, lineno) ]
                if (not self.do_translations) and comment and not t in self.comments:
                    self.comments[t] = comment
            else:
                if t in self.linenos.keys():
                    self.linenos[t].append((self.filename, tag, lineno))
                else:
                    self.linenos[t] = [ (self.filename, tag, lineno) ]
                if comment and not t in self.comments:
                    self.comments[t] = comment

    def outputHeader(self, out):
        import time
        out.write("""msgid ""
msgstr ""
"Project-Id-Version: PACKAGE VERSION\\n"
"POT-Creation-Date: %s\\n"
"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
"Language-Team: LANGUAGE <LL@li.org>\\n"
"MIME-Version: 1.0\\n"
"Content-Type: text/plain; charset=UTF-8\\n"
"Content-Transfer-Encoding: 8bit\\n"

""" % (time.strftime("%Y-%m-%d %H:%M%z")))

    def outputAll(self, out):
        self.outputHeader(out)

        for k in self.messages:
            if k in self.comments:
                out.write("#. %s\n" % (self.comments[k].replace("\n","\n#. ")))
            references = ""
            for reference in self.linenos[k]:
                references += "%s:%d(%s) " % (reference[0], reference[2], reference[1])
            out.write("#: %s\n" % (references))
            if k in self.nowrap and self.nowrap[k]:
                out.write("#, no-wrap\n")
            out.write("msgid \"%s\"\n" % (k))
            translation = ""
            if self.do_translations:
                if len(self.translations)>0:
                    translation = self.translations.pop(0)
            if translation == k:
                translation = ""
            out.write("msgstr \"%s\"\n\n" % (translation))


def normalizeNode(node):
    #print >>sys.stderr, "<%s> (%s) [%s]" % (node.name, node.type, node.serialize('utf-8'))
    if not node:
        return
    elif isSpacePreserveNode(node):
        return
    elif node.isText():
        if node.isBlankNode():
            if expand_entities or ( not (node.prev and not node.prev.isBlankNode()
                                         and node.next and not node.next.isBlankNode()) ):
                #print >>sys.stderr, "BLANK"
                node.setContent('')
        else:
            node.setContent(re.sub('\s+',' ', node.content))

    elif node.children and node.type == 'element':
        child = node.children
        while child:
            normalizeNode(child)
            child = child.next

def normalizeString(text, ignorewhitespace = 1):
    """Normalizes string to be used as key for gettext lookup.

    Removes all unnecessary whitespace."""
    if not ignorewhitespace:
        return text
    try:
        # Lets add document DTD so entities are resolved
        dtd = doc.intSubset()
        tmp = dtd.serialize('utf-8')
        tmp = tmp + '<norm>%s</norm>' % text
    except:
        tmp = '<norm>%s</norm>' % text

    try:
        ctxt = libxml2.createDocParserCtxt(tmp)
        if expand_entities:
            ctxt.replaceEntities(1)
        ctxt.parseDocument()
        tree = ctxt.doc()
        newnode = tree.getRootElement()
    except:
        print >> sys.stderr, """Error while normalizing string as XML:\n"%s"\n""" % (text)
        return text

    normalizeNode(newnode)

    result = ''
    child = newnode.children
    while child:
        result += child.serialize('utf-8')
        child = child.next

    result = re.sub('^ ','', result)
    result = re.sub(' $','', result)

    return result

def stringForEntity(node):
    """Replaces entities in the node."""
    text = node.serialize('utf-8')
    try:
        # Lets add document DTD so entities are resolved
        dtd = node.doc.intSubset()
        tmp = dtd.serialize('utf-8') + '<norm>%s</norm>' % text
        next = 1
    except:
        tmp = '<norm>%s</norm>' % text
        next = 0

    ctxt = libxml2.createDocParserCtxt(tmp)
    if expand_entities:
        ctxt.replaceEntities(1)
    ctxt.parseDocument()
    tree = ctxt.doc()
    if next:
        newnode = tree.children.next
    else:
        newnode = tree.children

    result = ''
    child = newnode.children
    while child:
        result += child.serialize('utf-8')
        child = child.next

    return result

# for writing in a po file
def escapePoString(text):
    return text.replace("\\'","'").replace('\\','\\\\').replace('\\\\"', '\\"').replace("\n","\\n").replace("\t","\\t")

# for turning xml into raw resource
def unEscapeXmlString(text):
    return text.replace('\\"','"').replace("\\'","'")

# for writing in an xml file
def escapeXmlString(text):
    return text.replace('"','\\"').replace("'","\\'")

def getTranslation(text, spacepreserve = 0):
    """Returns a translation via gettext for specified snippet.

    text should be a string to look for, spacepreserve set to 1
    when spaces should be preserved.
    """
    text = unEscapeXmlString(text)
    # print >>sys.stderr,"getTranslation('%s')" % (text.encode('utf-8'))
    text = normalizeString(text, not spacepreserve)
    if (text.strip() == ''):
        return text
    global gt
    if gt:
        res = gt.ugettext(text.decode('utf-8'))
        return res
    return text

def myAttributeSerialize(node):
    result = ''
    if node.children:
        child = node.children
        while child:
            if child.type=='text':
                result += doc.encodeEntitiesReentrant(child.content)
            elif child.type=='entity_ref':
                if not expand_entities:
                    result += '&' + child.name + ';'
                else:
                    result += child.content.decode('utf-8')
            else:
                result += myAttributeSerialize(child)
            child = child.next
    else:
        result = node.serialize('utf-8')
    return result

def startTagForNode(node):
    if not node:
        return 0

    result = node.name
    params = ''
    if node.properties:
        for p in node.properties:
            if p.type == 'attribute':
                try:
                    nsprop = p.ns().name + ":" + p.name
                except:
                    nsprop = p.name
                params += " %s=\"%s\"" % (nsprop, myAttributeSerialize(p))
    return result+params

def endTagForNode(node):
    if not node:
        return 0

    result = node.name
    return result

def isFinalNode(node):
    if automatic:
        auto = autoNodeIsFinal(node)
        # Check if any of the parents is also autoNodeIsFinal,
        # and if it is, don't consider this node a final one
        parent = node.parent
        while parent and auto:
            auto = not autoNodeIsFinal(parent)
            parent = parent.parent
        return auto
    #node.type =='text' or not node.children or
    if node.type == 'element' and node.name in ultimate_tags:
        return 1
    elif node.children:
        final_children = 1
        child = node.children
        while child and final_children:
            if not child.isBlankNode() and child.type != 'comment' and not isFinalNode(child):
                final_children = 0
            child = child.next
        if final_children:
            return 1
    return 0

def ignoreNode(node):
    if automatic:
        if node.type in ('dtd', 'comment'):
            return 1
        else:
            return 0
    else:
        if isFinalNode(node):
            return 0
        if node.name in ignored_tags or node.type in ('dtd', 'comment'):
            return 1
        return 0

def isSpacePreserveNode(node):
    pres = node.getSpacePreserve()
    if pres == 1:
        return 1
    else:
        if CurrentXmlMode and (node.name in CurrentXmlMode.getSpacePreserveTags()):
            return 1
        else:
            return 0

def getCommentForNode(node):
    """Walk through previous siblings until a comment is found, or other element.

    Only whitespace is allowed between comment and current node."""
    prev = node.prev
    while prev and prev.type == 'text' and prev.content.strip() == '':
        prev = prev.prev
    if prev and prev.type == 'comment':
        return prev.content.strip()
    else:
        return None

def replaceAttributeContentsWithText(node,text):
    node.setContent(text)

def replaceNodeContentsWithText(node,text):
    """Replaces all subnodes of a node with contents of text treated as XML."""

    if node.children:
        starttag = startTagForNode(node)
        endtag = endTagForNode(node)

        # Lets add document DTD so entities are resolved
        tmp = '<?xml version="1.0" encoding="utf-8" ?>'
        try:
            dtd = doc.intSubset()
            tmp = tmp + dtd.serialize('utf-8')
        except libxml2.treeError:
            pass

        content = '<%s>%s</%s>' % (starttag, text, endtag)
        tmp = tmp + content.encode('utf-8')

        newnode = None
        try:
            ctxt = libxml2.createDocParserCtxt(tmp)
            ctxt.replaceEntities(0)
            ctxt.parseDocument()
            newnode = ctxt.doc()
        except:
            pass

        if not newnode:
            print >> sys.stderr, """Error while parsing translation as XML:\n"%s"\n""" % (text.encode('utf-8'))
            return

        newelem = newnode.getRootElement()

        if newelem and newelem.children:
            free = node.children
            while free:
                next = free.next
                free.unlinkNode()
                free = next

            if node:
                copy = newelem.copyNodeList()
                next = node.next
                node.replaceNode(newelem.copyNodeList())
                node.next = next

        else:
            # In practice, this happens with tags such as "<para>    </para>" (only whitespace in between)
            pass
    else:
        node.setContent(text)

def autoNodeIsFinal(node):
    """Returns 1 if node is text node, contains non-whitespace text nodes or entities."""
    if hasattr(node, '__autofinal__'):
        return node.__autofinal__
    if node.name in ignored_tags:
        node.__autofinal__ = 0
        return 0
    if node.isText() and node.content.strip()!='':
        node.__autofinal__ = 1
        return 1
    final = 0
    child = node.children
    while child:
        if child.type in ['text'] and  child.content.strip()!='':
            final = 1
            break
        child = child.next

    node.__autofinal__ = final
    return final


def worthOutputting(node, noauto = 0):
    """Returns 1 if node is "worth outputting", otherwise 0.

    Node is "worth outputting", if none of the parents
    isFinalNode, and it contains non-blank text and entities.
    """
    if noauto and hasattr(node, '__worth__'):
        return node.__worth__
    elif not noauto and hasattr(node, '__autoworth__'):
        return node.__autoworth__
    worth = 1
    parent = node.parent
    final = isFinalNode(node) and node.name not in ignored_tags
    while not final and parent:
        if isFinalNode(parent):
            final = 1 # reset if we've got to one final tag
        if final and (parent.name not in ignored_tags) and worthOutputting(parent):
            worth = 0
            break
        parent = parent.parent
    if not worth:
        node.__worth__ = 0
        return 0

    if noauto:
        node.__worth__ = worth
        return worth
    else:
        node.__autoworth__ = autoNodeIsFinal(node)
        return node.__autoworth__

def processAttribute(node, attr):
    if not node or not attr or not worthOutputting(node=node, noauto=1):
        return

    outtxt = attr.content
    if mode=='merge':
        translation = getTranslation(outtxt, 0)
        replaceAttributeContentsWithText(attr, translation.encode('utf-8'))
    else:
        msg.outputMessage(outtxt, node.lineNo(),  "", 0,
                          node.name + ":" + attr.name)

def processElementTag(node, replacements, restart = 0):
    """Process node with node.type == 'element'."""
    if node.type == 'element':
        # Translate attributes if needed
        if node.properties and len(treated_attributes):
            for p in node.properties:
                if p.name in treated_attributes:
                    processAttribute(node, p)

        outtxt = ''
        if restart:
            myrepl = []
        else:
            myrepl = replacements

        submsgs = []

        child = node.children
        while child:
            if (isFinalNode(child)) or (child.type == 'element' and worthOutputting(child)):
                myrepl.append(processElementTag(child, myrepl, 1))
                outtxt += '<placeholder-%d/>' % (len(myrepl))
            else:
                if child.type == 'element':
                    (starttag, content, endtag, translation) = processElementTag(child, myrepl, 0)
                    outtxt += '<%s>%s</%s>' % (starttag, content, endtag)
                else:
                    outtxt += doSerialize(child)

            child = child.next

        if mode == 'merge':
            translation = getTranslation(outtxt, isSpacePreserveNode(node))
        else:
            translation = outtxt.decode('utf-8')

        starttag = startTagForNode(node)
        endtag = endTagForNode(node)

        worth = worthOutputting(node)
        if not translation:
            translation = outtxt.decode('utf-8')
            if worth and mark_untranslated: node.setLang('C')
        else:
            translation = escapeXmlString(translation)
        
        if restart or worth:
            i = 0
            while i < len(myrepl):
                replacement = '<%s>%s</%s>' % (myrepl[i][0], myrepl[i][3], myrepl[i][2])
                i += 1
                translation = translation.replace('<placeholder-%d/>' % (i), replacement)

            if worth:
                if mode == 'merge':
                    replaceNodeContentsWithText(node, translation)
                else:
                    # try setting tag to the attribute's property
                    tag = node.name
                    if not node.properties is None:
                        tag = node.properties
                    msg.outputMessage(outtxt, node.lineNo(), getCommentForNode(node), isSpacePreserveNode(node), tag = tag)

        return (starttag, outtxt, endtag, translation)
    else:
        raise Exception("You must pass node with node.type=='element'.")


def isExternalGeneralParsedEntity(node):
    if (node and node.type=='entity_ref'):
        try:
            # it would be nice if debugDumpNode could use StringIO, but it apparently cannot
            tmp = file(".xml2po-entitychecking","w+")
            node.debugDumpNode(tmp,0)
            tmp.seek(0)
            tmpstr = tmp.read()
            tmp.close()
            os.remove(".xml2po-entitychecking")
        except:
            # We fail silently, and replace all entities if we cannot
            # write .xml2po-entitychecking
            # !!! This is not very nice thing to do, but I don't know if
            #     raising an exception is any better
            return 0
        if tmpstr.find('EXTERNAL_GENERAL_PARSED_ENTITY') != -1:
            return 1
        else:
            return 0
    else:
        return 0

def doSerialize(node):
    """Serializes a node and its children, emitting PO messages along the way.

    node is the node to serialize, first indicates whether surrounding
    tags should be emitted as well.
    """

    if ignoreNode(node):
        return ''
    elif not node.children:
        return node.serialize("utf-8")
    elif node.type == 'entity_ref':
        if isExternalGeneralParsedEntity(node):
            return node.serialize('utf-8')
        else:
            return stringForEntity(node) #content #content #serialize("utf-8")
    elif node.type == 'entity_decl':
        return node.serialize('utf-8') #'<%s>%s</%s>' % (startTagForNode(node), node.content, node.name)
    elif node.type == 'text':
        return node.serialize('utf-8')
    elif node.type == 'element':
        repl = []
        (starttag, content, endtag, translation) = processElementTag(node, repl, 1)
        return '<%s>%s</%s>' % (starttag, content, endtag)
    else:
        child = node.children
        outtxt = ''
        while child:
            outtxt += doSerialize(child)
            child = child.next
        return outtxt


def read_finaltags(filelist):
    if CurrentXmlMode:
        return CurrentXmlMode.getFinalTags()
    else:
        defaults = ['para', 'title', 'releaseinfo', 'revnumber',
                    'date', 'itemizedlist', 'orderedlist',
                    'variablelist', 'varlistentry', 'term' ]
        return defaults

def read_ignoredtags(filelist):
    if CurrentXmlMode:
        return CurrentXmlMode.getIgnoredTags()
    else:
        defaults = ['itemizedlist', 'orderedlist', 'variablelist',
                    'varlistentry' ]
        return defaults

def read_treatedattributes(filelist):
    if CurrentXmlMode:
        return CurrentXmlMode.getTreatedAttributes()
    else:
        return []


def tryToUpdate(allargs, lang):
    # Remove "-u" and "--update-translation"
    print >>sys.stderr, "OVDI!"
    command = allargs[0]
    args = allargs[1:]
    opts, args = getopt.getopt(args, 'avhm:ket:o:p:u:r:',
                               ['automatic-tags','version', 'help', 'keep-entities', 'extract-all-entities', 'merge', 'translation=',
                                'output=', 'po-file=', 'update-translation=', "reuse=" ])
    for opt, arg in opts:
        if opt in ('-a', '--automatic-tags'):
            command += " -a"
        elif opt in ('-k', '--keep-entities'):
            command += " -k"
        elif opt in ('-e', '--extract-all-entities'):
            command += " -e"
        elif opt in ('-r', '--reuse'):
            origxml = arg
        elif opt in ('-m', '--mode'):
            command += " -m %s" % arg
        elif opt in ('-o', '--output'):
            sys.stderr.write("Error: Option '-o' is not yet supported when updating translations directly.\n")
            sys.exit(8)
        elif opt in ('-v', '--version'):
            print VERSION
            sys.exit(0)
        elif opt in ('-h', '--help'):
            sys.stderr.write("Error: If you want help, please use `%s --help' without '-u' option.\n" % (allargs[0]))
            sys.exit(9)
        elif opt in ('-u', '--update-translation'):
            pass
        else:
            sys.stderr.write("Error: Option `%s' is not supported with option `-u'.\n" % (opt))
            sys.exit(9)

    while args:
        command += " " + args.pop()

    file = lang

    sys.stderr.write("Merging translations for %s: " % (lang))
    result = os.system("%s | msgmerge -o .tmp.%s.po %s -" % (command, lang, file))
    if result:
        sys.exit(10)
    else:
        result = os.system("mv .tmp.%s.po %s" % (lang, file))
        if result:
            sys.stderr.write("Error: cannot rename file.\n")
            sys.exit(11)
        else:
            os.system("msgfmt -cv -o %s %s" % (NULL_STRING, file))
            sys.exit(0)

def load_mode(modename):
    #import imp
    #found = imp.find_module(modename, submodes_path)
    #module = imp.load_module(modename, found[0], found[1], found[2])
    try:
        sys.path.append(submodes_path)
        module = __import__(modename)
        modeModule = '%sXmlMode' % modename
        return getattr(module, modeModule)
    except:
        return None

def xml_error_handler(arg, ctxt):
    pass

libxml2.registerErrorHandler(xml_error_handler, None)


# Main program start
if __name__ != '__main__': raise NotImplementedError

# Parameters
submodes_path = "modes"
default_mode = 'docbook'

filename = ''
origxml = ''
mofile = ''
gt = None
ultimate = [ ]
ignored = [ ]
filenames = [ ]
translationlanguage = ''

mode = 'pot' # 'pot' or 'merge'
automatic = 0
expand_entities = 1
mark_untranslated = 0
expand_all_entities = 0

output  = '-' # this means to stdout

NULL_STRING = '/dev/null'
if not os.path.exists('/dev/null'): NULL_STRING = 'NUL'

import getopt, fileinput

def usage (with_help = False):
        print >> sys.stderr, "Usage:  %s [OPTIONS] [XMLFILE]..." % (sys.argv[0])
	if (with_help):
        	print >> sys.stderr, """
OPTIONS may be some of:
    -a    --automatic-tags     Automatically decides if tags are to be considered
                                 "final" or not
    -k    --keep-entities      Don't expand entities
    -e    --expand-all-entities  Expand ALL entities (including SYSTEM ones)
    -m    --mode=TYPE          Treat tags as type TYPE (default: docbook)
    -o    --output=FILE        Print resulting text (XML or POT) to FILE
    -p    --po-file=FILE       Specify PO file containing translation, and merge
                                 Overwrites temporary file .xml2po.mo.
    -r    --reuse=FILE         Specify translated XML file with the same structure
    -t    --translation=FILE   Specify MO file containing translation, and merge
    -u    --update-translation=LANG.po   Updates a PO file using msgmerge program

    -l    --language=LANG      Set language of the translation to LANG
          --mark-untranslated  Set 'xml:lang="C"' on untranslated tags

    -v    --version            Output version of the xml2po program

    -h    --help               Output this message

EXAMPLES:
    To create a POTemplate book.pot from input files chapter1.xml and
    chapter2.xml, run the following:
        %s -o book.pot chapter1.xml chapter2.xml

    After translating book.pot into de.po, merge the translations back,
    using -p option for each XML file:
        %s -p de.po chapter1.xml > chapter1.de.xml
        %s -p de.po chapter2.xml > chapter2.de.xml
""" % (sys.argv[0], sys.argv[0], sys.argv[0])
        sys.exit(0)

if len(sys.argv) < 2: usage()

args = sys.argv[1:]
try: opts, args = getopt.getopt(args, 'avhkem:t:o:p:u:r:l:',
                           ['automatic-tags','version', 'help', 'keep-entities', 'expand-all-entities', 'mode=', 'translation=',
                            'output=', 'po-file=', 'update-translation=', 'reuse=', 'language=', 'mark-untranslated' ])
except getopt.GetoptError: usage(True)

for opt, arg in opts:
    if opt in ('-m', '--mode'):
        default_mode = arg
    if opt in ('-a', '--automatic-tags'):
        automatic = 1
    elif opt in ('-k', '--keep-entities'):
        expand_entities = 0
    elif opt in ('--mark-untranslated',):
        mark_untranslated = 1
    elif opt in ('-e', '--expand-all-entities'):
        expand_all_entities = 1
    elif opt in ('-l', '--language'):
        translationlanguage = arg
    elif opt in ('-t', '--translation'):
        mofile = arg
        mode = 'merge'
        if translationlanguage == '': translationlanguage = os.path.split(os.path.splitext(mofile)[0])[1]
    elif opt in ('-r', '--reuse'):
        origxml = arg
    elif opt in ('-u', '--update-translation'):
        tryToUpdate(sys.argv, arg)
    elif opt in ('-p', '--po-file'):
        mofile = ".xml2po.mo"
        pofile = arg
        if translationlanguage == '': translationlanguage = os.path.split(os.path.splitext(pofile)[0])[1]
        os.system("msgfmt -o %s %s >%s" % (mofile, pofile, NULL_STRING)) and sys.exit(7)
        mode = 'merge'
    elif opt in ('-o', '--output'):
        output = arg
    elif opt in ('-v', '--version'):
        print VERSION
        sys.exit(0)
    elif opt in ('-h', '--help'):
    	usage(True)

# Treat remaining arguments as XML files
while args:
    filenames.append(args.pop())

if len(filenames) > 1 and mode=='merge':
    print  >> sys.stderr, "Error: You can merge translations with only one XML file at a time."
    sys.exit(2)

try:
    CurrentXmlMode = load_mode(default_mode)()
except:
    CurrentXmlMode = None
    # print >> sys.stderr, "Warning: cannot load module '%s', using automatic detection (-a)." % (default_mode)
    automatic = 1

if mode=='merge' and mofile=='':
    print >> sys.stderr, "Error: You must specify MO file when merging translations."
    sys.exit(3)

if mofile:
    try:
        mfile = open(mofile, "rb")

        gt = gettext.GNUTranslations(mfile)
        gt.add_fallback(NoneTranslations())
    except:
        print >> sys.stderr, "Can't open MO file '%s'." % (mofile)

ultimate_tags = read_finaltags(ultimate)
ignored_tags = read_ignoredtags(ignored)
treated_attributes = read_treatedattributes(ignored)

# I'm not particularly happy about making any of these global,
# but I don't want to bother too much with it right now
semitrans = {}
PlaceHolder = 0
if origxml == '':
    msg = MessageOutput()
else:
    filenames.append(origxml)
    msg = MessageOutput(1)

for filename in filenames:
    try:
        if filename == origxml:
            msg.translationsFollow()
        ctxt = libxml2.createFileParserCtxt(filename)
        ctxt.lineNumbers(1)
        if expand_all_entities:
            ctxt.replaceEntities(1)
        ctxt.parseDocument()
        doc = ctxt.doc()
        if doc.name != filename:
            print >> sys.stderr, "Error: I tried to open '%s' but got '%s' -- how did that happen?" % (filename, doc.name)
            sys.exit(4)
    except:
        print >> sys.stderr, "Error: cannot open file '%s'." % (filename)
        sys.exit(1)

    msg.setFilename(filename)
    if CurrentXmlMode and origxml=='':
        CurrentXmlMode.preProcessXml(doc,msg)
    doSerialize(doc)

if output == '-':
    out = sys.stdout
else:
    try:
        out = file(output, 'w')
    except:
        print >> sys.stderr, "Error: cannot open file %s for writing." % (output)
        sys.exit(5)

if mode != 'merge':
    if CurrentXmlMode:
        tcmsg = CurrentXmlMode.getStringForTranslators()
        tccom = CurrentXmlMode.getCommentForTranslators()
        if tcmsg:
            msg.outputMessage(tcmsg, 0, tccom)

    msg.outputAll(out)
else:
    if CurrentXmlMode:
        tcmsg = CurrentXmlMode.getStringForTranslators()
        if tcmsg:
            outtxt = getTranslation(tcmsg)
        else:
            outtxt = ''
        CurrentXmlMode.postProcessXmlTranslation(doc, translationlanguage, outtxt)
    out.write(doc.serialize('utf-8', 1))
