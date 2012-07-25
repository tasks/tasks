/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.mdimension.jchronic;

import java.util.LinkedList;
import java.util.List;

import com.mdimension.jchronic.handlers.Handler;
import com.mdimension.jchronic.numerizer.Numerizer;
import com.mdimension.jchronic.repeaters.Repeater;
import com.mdimension.jchronic.tags.Grabber;
import com.mdimension.jchronic.tags.Ordinal;
import com.mdimension.jchronic.tags.Pointer;
import com.mdimension.jchronic.tags.Scalar;
import com.mdimension.jchronic.tags.Separator;
import com.mdimension.jchronic.tags.TimeZone;
import com.mdimension.jchronic.utils.Span;
import com.mdimension.jchronic.utils.Token;

@SuppressWarnings("nls")
public class AstridChronic {
  public static final String VERSION = "0.2.3";

  private AstridChronic() {
    // DO NOTHING
  }

  public static Span parse(String text) {
    return AstridChronic.parse(text, new Options());
  }

  /**
   * Parses a string containing a natural language date or time. If the parser
   * can find a date or time, either a Time or Chronic::Span will be returned
   * (depending on the value of <tt>:guess</tt>). If no date or time can be found,
   * +nil+ will be returned.
   *
   * Options are:
   *
   * [<tt>:context</tt>]
   *     <tt>:past</tt> or <tt>:future</tt> (defaults to <tt>:future</tt>)
   *
   *     If your string represents a birthday, you can set <tt>:context</tt> to <tt>:past</tt>
   *     and if an ambiguous string is given, it will assume it is in the
   *     past. Specify <tt>:future</tt> or omit to set a future context.
   *
   * [<tt>:now</tt>]
   *     Time (defaults to Time.now)
   *
   *     By setting <tt>:now</tt> to a Time, all computations will be based off
   *     of that time instead of Time.now
   *
   * [<tt>:guess</tt>]
   *     +true+ or +false+ (defaults to +true+)
   *
   *     By default, the parser will guess a single point in time for the
   *     given date or time. If you'd rather have the entire time span returned,
   *     set <tt>:guess</tt> to +false+ and a Chronic::Span will be returned.
   *
   * [<tt>:ambiguous_time_range</tt>]
   *     Integer or <tt>:none</tt> (defaults to <tt>6</tt> (6am-6pm))
   *
   *     If an Integer is given, ambiguous times (like 5:00) will be
   *     assumed to be within the range of that time in the AM to that time
   *     in the PM. For example, if you set it to <tt>7</tt>, then the parser will
   *     look for the time between 7am and 7pm. In the case of 5:00, it would
   *     assume that means 5:00pm. If <tt>:none</tt> is given, no assumption
   *     will be made, and the first matching instance of that time will
   *     be used.
   */
  @SuppressWarnings("unchecked")
  public static Span parse(String text, Options options) {
    // store now for later =)
    //_now = options.getNow();

    // put the text into a normal format to ease scanning
    String normalizedText = AstridChronic.preNormalize(text);

    // get base tokens for each word
    List<Token> tokens = AstridChronic.baseTokenize(normalizedText);

    try {
        tokens = Repeater.scan(tokens, options);
    } catch (Throwable e) {
        throw new RuntimeException("Failed to scan tokens.", e);
    }

    tokens = Grabber.scan(tokens, options);
    tokens = Pointer.scan(tokens, options);
    tokens = Scalar.scan(tokens, options);
    tokens = Ordinal.scan(tokens, options);
    tokens = Separator.scan(tokens, options);
    tokens = TimeZone.scan(tokens, options);

    List<Token> taggedTokens = new LinkedList<Token>();
    for (Token token : tokens) {
      if (token.isTagged()) {
        taggedTokens.add(token);
      }
    }
    tokens = taggedTokens;

    if (options.isDebug()) {
      System.out.println("Chronic.parse: " + tokens);
    }

    Span span = Handler.tokensToSpan(tokens, options);

    // guess a time within a span if required
    if (options.isGuess()) {
      span = guess(span);
    }

    return span;
  }

  /**
   * Clean up the specified input text by stripping unwanted characters,
   * converting idioms to their canonical form, converting number words
   * to numbers (three => 3), and converting ordinal words to numeric
   * ordinals (third => 3rd)
   */
  protected static String preNormalize(String text) {
    String normalizedText = text.toLowerCase();
    normalizedText = Chronic.numericizeNumbers(normalizedText);
    normalizedText = normalizedText.replaceAll("['\"\\.]", "");
    normalizedText = normalizedText.replaceAll("([/\\-,@])", " $1 ");
    normalizedText = normalizedText.replaceAll("\\btoday\\b", "this day");
    normalizedText = normalizedText.replaceAll("\\btomm?orr?ow\\b", "next day");
    normalizedText = normalizedText.replaceAll("\\byesterday\\b", "last day");
    normalizedText = normalizedText.replaceAll("\\bnoon\\b", "12:00");
    normalizedText = normalizedText.replaceAll("\\bmidnight\\b", "24:00");
    normalizedText = normalizedText.replaceAll("\\bbefore now\\b", "past");
    normalizedText = normalizedText.replaceAll("\\bnow\\b", "this second");
    normalizedText = normalizedText.replaceAll("\\b(ago|before)\\b", "past");
    normalizedText = normalizedText.replaceAll("\\bthis past\\b", "last");
    normalizedText = normalizedText.replaceAll("\\bthis last\\b", "last");
    normalizedText = normalizedText.replaceAll("\\b(?:in|during) the (morning)\\b", "$1");
    normalizedText = normalizedText.replaceAll("\\b(?:in the|during the|at) (afternoon|evening|night)\\b", "$1");
    normalizedText = normalizedText.replaceAll("\\btonight\\b", "this night");
    normalizedText = normalizedText.replaceAll("(?=\\w)([ap]m|oclock)\\b", " $1");
    normalizedText = normalizedText.replaceAll("\\b(hence|after|from)\\b", "future");
    normalizedText = AstridChronic.numericizeOrdinals(normalizedText);
    return normalizedText;
  }

  /**
   * Convert number words to numbers (three => 3)
   */
  protected static String numericizeNumbers(String text) {
    return Numerizer.numerize(text);
  }

  /**
   * Convert ordinal words to numeric ordinals (third => 3rd)
   */
  protected static String numericizeOrdinals(String text) {
    return text;
  }

  /**
   * Split the text on spaces and convert each word into
   * a Token
   */
  protected static List<Token> baseTokenize(String text) {
    String[] words = text.split(" ");
    List<Token> tokens = new LinkedList<Token>();
    for (String word : words) {
      tokens.add(new Token(word));
    }
    return tokens;
  }

  /**
   * Guess a specific time within the given span
   */
  // DIFF: We return Span instead of Date
  protected static Span guess(Span span) {
    if (span == null) {
      return null;
    }
    long guessValue;
    if (span.getWidth() > 1) {
      guessValue = span.getBegin() + (span.getWidth() / 2);
    }
    else {
      guessValue = span.getBegin();
    }
    Span guess = new Span(guessValue, guessValue);
    return guess;
  }
}
