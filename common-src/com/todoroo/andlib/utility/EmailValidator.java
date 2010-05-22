package com.todoroo.andlib.utility;

import java.util.regex.Pattern;

/**
 * E-mail Validator Copyright 2008 Les Hazlewood
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
@SuppressWarnings("nls")
public final class EmailValidator {

    // RFC 2822 2.2.2 Structured Header Field Bodies
    private static final String wsp = "[ \\t]"; // space or tab
    private static final String fwsp = wsp + "*";

    // RFC 2822 3.2.1 Primitive tokens
    private static final String dquote = "\\\"";
    // ASCII Control characters excluding white space:
    private static final String noWsCtl = "\\x01-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F";
    // all ASCII characters except CR and LF:
    private static final String asciiText = "[\\x01-\\x09\\x0B\\x0C\\x0E-\\x7F]";

    // RFC 2822 3.2.2 Quoted characters:
    // single backslash followed by a text char
    private static final String quotedPair = "(\\\\" + asciiText + ")";

    // RFC 2822 3.2.4 Atom:
    private static final String atext = "[a-zA-Z0-9\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~]";
    private static final String dotAtomText = atext + "+" + "(" + "\\." + atext
            + "+)*";
    private static final String dotAtom = fwsp + "(" + dotAtomText + ")" + fwsp;

    // RFC 2822 3.2.5 Quoted strings:
    // noWsCtl and the rest of ASCII except the doublequote and backslash
    // characters:
    private static final String qtext = "[" + noWsCtl
            + "\\x21\\x23-\\x5B\\x5D-\\x7E]";
    private static final String qcontent = "(" + qtext + "|" + quotedPair + ")";
    private static final String quotedString = dquote + "(" + fwsp + qcontent
            + ")*" + fwsp + dquote;

    // RFC 1035 tokens for domain names:
    private static final String letter = "[a-zA-Z]";
    private static final String letDig = "[a-zA-Z0-9]";
    private static final String letDigHyp = "[a-zA-Z0-9-]";
    private static final String rfcLabel = letDig + "(" + letDigHyp + "{0,61}"
            + letDig + ")?";
    private static final String rfc1035DomainName = rfcLabel + "(\\."
            + rfcLabel + ")*\\." + letter + "{2,6}";

    private static final String domain = rfc1035DomainName;

    private static final String localPart = "((" + dotAtom + ")|("
            + quotedString + "))";
    private static final String addrSpec = localPart + "@" + domain;

    // now compile a pattern for efficient re-use:
    // if we're allowing quoted identifiers or not:
    private static final String patternString = addrSpec;
    public static final Pattern VALID_PATTERN = Pattern.compile(patternString);


    public static boolean validateEmail(String value) {
        return VALID_PATTERN.matcher(value).matches();
    }
}