package com.nido.api.calendar.domain.model;

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

    private static final Pattern TAG = Pattern.compile("<[a-zA-Z/!][^<>]*>");
    /** The whole opening tag, possessive: it ends at the next {@code <} or {@code >}, and never backtracks. */
    private static final Pattern LINK_OPEN = Pattern.compile("(?i)<a(?=\\s)[^<>]*+>");
    /**
     * The first quoted href of a tag's text. A value stops at the next quote of either kind, so an attempt scans only up
     * to the next quote, and attempts start right after a quote: the stretches they scan never overlap.
     */
    private static final Pattern HREF = Pattern.compile("(?i)\\bhref\\s*=\\s*(?:\"([^\"']*)\"|'([^\"']*)')");
    private static final Pattern LINK_CLOSE = Pattern.compile("(?i)</a\\s*>");
    private static final Pattern BLOCK_OPEN = Pattern.compile("(?i)<!--|<(script|style)(?:\\s[^<>]*)?>");
    private static final Pattern SCRIPT_CLOSE = Pattern.compile("(?i)</script\\s*>");
    private static final Pattern STYLE_CLOSE = Pattern.compile("(?i)</style\\s*>");
    private static final Pattern BREAK =
        Pattern.compile("(?i)<br\\s*/?>|<p(\\s[^<>]*)?>|</p\\s*>|</div\\s*>|</li\\s*>|</h[1-6]\\s*>");
    private static final Pattern ENTITY = Pattern.compile("&(#[xX][0-9a-fA-F]{1,6}|#[0-9]{1,7}|[a-zA-Z]{1,10});");
    private static final Map<String, String> NAMED = Map.of(
        "amp", "&", "lt", "<", "gt", ">", "quot", "\"", "apos", "'", "nbsp", " ");

    private HtmlText() {}

    public static boolean looksLikeHtml(String text) {
        return text != null && TAG.matcher(text).find();
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
            boolean keepable = Character.isValidCodePoint(codePoint)
                && !(codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE)
                && (!Character.isISOControl(codePoint) || codePoint == '\t' || codePoint == '\n');
            return keepable ? Character.toString(codePoint) : written;
        }
        return NAMED.getOrDefault(entity.toLowerCase(), written);
    }
}
