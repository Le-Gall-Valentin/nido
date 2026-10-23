package com.nido.api.calendar.domain.model;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The text an HTML description shows — Google writes its descriptions in HTML. Stored and shown as plain text, never
 * as HTML: a line break stays one, a link keeps its address, and nothing of the markup survives to be interpreted.
 * Our own and small, as jsoup is the mail module's alone.
 *
 * <p>The text comes from a file nobody vouches for, so every step is linear in its length: no pattern lets a failed
 * attempt scan past the next {@code <}, and what has to look for a closing tag (a link, a script, a comment) looks once
 * for the rest of the text, never again after a miss.
 */
public final class HtmlText {

    /**
     * A tag: a real element name (Outlook's {@code o:p} included), then a blank, a slash or its end — so neither an
     * address written between angle brackets ({@code <https://…>}, {@code <jeanne@example.com>}) nor a comparison is
     * one —, or a declaration ({@code <!DOCTYPE …>}).
     */
    private static final Pattern TAG = Pattern.compile("</?[a-zA-Z][a-zA-Z0-9]*+(?::[a-zA-Z][a-zA-Z0-9]*+)?(?:[\\s/][^<>]*+)?>|<![^<>]*+>");
    /**
     * What makes a text HTML: a tag of an element descriptions are written with. A plain text may hold anything between
     * angle brackets — "x<y et y>z" —, never one of these.
     */
    private static final Pattern HTML_TAG = Pattern.compile("(?i)</?(?:a|abbr|b|big|blockquote|body|br|center|code|dd|div|dl|dt|em|"
        + "font|h[1-6]|head|hr|html|i|img|li|meta|o:p|ol|p|pre|s|small|span|strike|strong|style|sub|sup|table|tbody|td|th|"
        + "thead|title|tr|u|ul)(?=[\\s/>])[^<>]*+>");
    /** The whole opening tag, possessive: it ends at the next {@code <} or {@code >}, and never backtracks. */
    private static final Pattern LINK_OPEN = Pattern.compile("(?i)<a(?=\\s)[^<>]*+>");
    /**
     * The first quoted href of a tag's text. A value runs to the next quote of its own kind — "l'atelier" keeps its
     * apostrophe. An attempt that finds that quote ends the search; one that does not scans to the end, and only the last
     * opening quote of each kind can: the search stays linear.
     */
    private static final Pattern HREF = Pattern.compile("(?i)\\bhref\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");
    private static final Pattern LINK_CLOSE = Pattern.compile("(?i)</a\\s*>");
    private static final Pattern BLOCK_OPEN = Pattern.compile("(?i)<!--|<(script|style)(?:\\s[^<>]*)?>");
    private static final Pattern SCRIPT_CLOSE = Pattern.compile("(?i)</script\\s*>");
    private static final Pattern STYLE_CLOSE = Pattern.compile("(?i)</style\\s*>");
    private static final Pattern BREAK =
        Pattern.compile("(?i)<br\\s*/?>|<p(\\s[^<>]*)?>|</p\\s*>|</div\\s*>|</li\\s*>|</h[1-6]\\s*>");
    private static final Pattern ENTITY = Pattern.compile("&(#[xX][0-9a-fA-F]{1,6}|#[0-9]{1,7}|[a-zA-Z][a-zA-Z0-9]{0,9});");
    /** HTML's names of the Latin-1 characters, from U+00A0 on, in order. */
    private static final String LATIN_1 = "nbsp iexcl cent pound curren yen brvbar sect uml copy ordf laquo not shy reg macr "
        + "deg plusmn sup2 sup3 acute micro para middot cedil sup1 ordm raquo frac14 frac12 frac34 iquest Agrave Aacute Acirc "
        + "Atilde Auml Aring AElig Ccedil Egrave Eacute Ecirc Euml Igrave Iacute Icirc Iuml ETH Ntilde Ograve Oacute Ocirc "
        + "Otilde Ouml times Oslash Ugrave Uacute Ucirc Uuml Yacute THORN szlig agrave aacute acirc atilde auml aring aelig "
        + "ccedil egrave eacute ecirc euml igrave iacute icirc iuml eth ntilde ograve oacute ocirc otilde ouml divide oslash "
        + "ugrave uacute ucirc uuml yacute thorn yuml";
    /** The others a description is written with: markup, typography, French ligatures, the euro. */
    private static final Map<String, Integer> OTHERS = Map.ofEntries(
        Map.entry("amp", (int) '&'), Map.entry("lt", (int) '<'), Map.entry("gt", (int) '>'), Map.entry("quot", (int) '"'),
        Map.entry("apos", (int) '\''), Map.entry("OElig", 0x152), Map.entry("oelig", 0x153), Map.entry("Scaron", 0x160),
        Map.entry("scaron", 0x161), Map.entry("Yuml", 0x178), Map.entry("fnof", 0x192), Map.entry("circ", 0x2C6),
        Map.entry("tilde", 0x2DC), Map.entry("ensp", 0x2002), Map.entry("emsp", 0x2003), Map.entry("thinsp", 0x2009),
        Map.entry("ndash", 0x2013), Map.entry("mdash", 0x2014), Map.entry("lsquo", 0x2018), Map.entry("rsquo", 0x2019),
        Map.entry("sbquo", 0x201A), Map.entry("ldquo", 0x201C), Map.entry("rdquo", 0x201D), Map.entry("bdquo", 0x201E),
        Map.entry("dagger", 0x2020), Map.entry("Dagger", 0x2021), Map.entry("bull", 0x2022), Map.entry("hellip", 0x2026),
        Map.entry("permil", 0x2030), Map.entry("prime", 0x2032), Map.entry("Prime", 0x2033), Map.entry("lsaquo", 0x2039),
        Map.entry("rsaquo", 0x203A), Map.entry("euro", 0x20AC), Map.entry("trade", 0x2122), Map.entry("larr", 0x2190),
        Map.entry("rarr", 0x2192));
    private static final Map<String, String> NAMED = named();
    /** A carriage return written as an entity: Outlook ends its lines with {@code &#13;} before their line feed. */
    private static final int CARRIAGE_RETURN = 13;

    private HtmlText() {}

    public static boolean looksLikeHtml(String text) {
        return text != null && HTML_TAG.matcher(text).find();
    }

    public static String toPlain(String html) {
        String text = links(dropBlocks(html));
        text = BREAK.matcher(text).replaceAll("\n");
        text = TAG.matcher(text).replaceAll("");
        text = ENTITY.matcher(text).replaceAll(match -> Matcher.quoteReplacement(decode(match.group(), match.group(1))));
        return trimLines(text).replaceAll("\\n{3,}", "\n\n").strip();
    }

    /**
     * Scripts, styles and comments go with their content. One that is never closed takes the rest of the text with it:
     * nothing after it can be told from its content.
     */
    private static String dropBlocks(String html) {
        Matcher open = BLOCK_OPEN.matcher(html);
        StringBuilder out = new StringBuilder(html.length());
        int from = 0;
        while (open.find(from)) {
            out.append(html, from, open.start());
            int close;
            if (open.group(1) == null) {
                close = html.indexOf("-->", open.end());
                from = close < 0 ? html.length() : close + 3;
            } else {
                Matcher end = ("script".equalsIgnoreCase(open.group(1)) ? SCRIPT_CLOSE : STYLE_CLOSE).matcher(html);
                from = end.find(open.end()) ? end.end() : html.length();
            }
            if (from >= html.length()) {
                return out.toString();
            }
        }
        return out.append(html, from, html.length()).toString();
    }

    /**
     * A link becomes its label and its address. Written by hand rather than as one pattern with a lazy label: that one
     * scans to the end of the text for every link left open. Here the closing tag is searched for once, and when the
     * text holds none, no later link searches again.
     */
    private static String links(String html) {
        Matcher open = LINK_OPEN.matcher(html);
        Matcher close = LINK_CLOSE.matcher(html);
        StringBuilder out = new StringBuilder(html.length());
        int from = 0;
        int search = 0;
        while (open.find(search)) {
            search = open.end();
            Matcher href = HREF.matcher(open.group());
            if (!href.find()) {
                continue;
            }
            if (!close.find(open.end())) {
                break;
            }
            out.append(html, from, open.start());
            String address = (href.group(1) != null ? href.group(1) : href.group(2)).strip();
            String label = TAG.matcher(html.substring(open.end(), close.start())).replaceAll("").strip();
            out.append(label.isEmpty() || label.equals(address) ? address : label + " (" + address + ")");
            from = close.end();
            search = from;
        }
        return out.append(html, from, html.length()).toString();
    }

    /** Each line without the blanks it ends with. */
    private static String trimLines(String text) {
        StringBuilder out = new StringBuilder(text.length());
        int lineStart = 0;
        while (lineStart <= text.length()) {
            int lineEnd = text.indexOf('\n', lineStart);
            int end = lineEnd < 0 ? text.length() : lineEnd;
            int keep = end;
            while (keep > lineStart && (text.charAt(keep - 1) == ' ' || text.charAt(keep - 1) == '\t')) {
                keep--;
            }
            out.append(text, lineStart, keep);
            if (lineEnd < 0) {
                break;
            }
            out.append('\n');
            lineStart = lineEnd + 1;
        }
        return out.toString();
    }

    /** What an entity stands for, or {@code written} when it names nothing nido can keep as text. */
    private static String decode(String written, String entity) {
        if (entity.startsWith("#")) {
            boolean hex = entity.startsWith("#x") || entity.startsWith("#X");
            int codePoint = Integer.parseInt(entity.substring(hex ? 2 : 1), hex ? 16 : 10);
            if (codePoint == CARRIAGE_RETURN) {
                return "";
            }
            boolean keepable = Character.isValidCodePoint(codePoint)
                && !(codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE)
                && (!Character.isISOControl(codePoint) || codePoint == '\t' || codePoint == '\n');
            return keepable ? Character.toString(codePoint) : written;
        }
        // Exact first: &Eacute; is not &eacute;. Browsers also read &AMP; and the like, written in capitals.
        return NAMED.getOrDefault(entity, NAMED.getOrDefault(entity.toLowerCase(), written));
    }

    private static Map<String, String> named() {
        Map<String, String> named = new HashMap<>();
        String[] latin1 = LATIN_1.split(" ");
        for (int i = 0; i < latin1.length; i++) {
            named.put(latin1[i], Character.toString(0xA0 + i));
        }
        OTHERS.forEach((name, codePoint) -> named.put(name, Character.toString(codePoint)));
        named.put("nbsp", " "); // a space as any other: a description is no layout
        return Map.copyOf(named);
    }
}
