package com.nido.api.calendar.domain.model;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The text an HTML description shows — Google writes its descriptions in HTML. Stored and shown as plain text, never
 * as HTML: a line break stays one, a link keeps its address, and nothing of the markup survives to be interpreted.
 * Our own and small, as jsoup is the mail module's alone.
 */
public final class HtmlText {

    private static final Pattern TAG = Pattern.compile("<[a-zA-Z/!][^>]*>");
    private static final Pattern LINK = Pattern.compile("(?is)<a\\s[^>]*href\\s*=\\s*\"([^\"]*)\"[^>]*>(.*?)</a\\s*>");
    private static final Pattern BREAK = Pattern.compile("(?i)<br\\s*/?>|<p(\\s[^>]*)?>|</p\\s*>|</div\\s*>|</li\\s*>|</h[1-6]\\s*>");
    private static final Pattern ENTITY = Pattern.compile("&(#[xX][0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);");
    private static final Map<String, String> NAMED = Map.of(
        "amp", "&", "lt", "<", "gt", ">", "quot", "\"", "apos", "'", "nbsp", " ");

    private HtmlText() {}

    public static boolean looksLikeHtml(String text) {
        return text != null && TAG.matcher(text).find();
    }

    public static String toPlain(String html) {
        String text = LINK.matcher(html).replaceAll(match -> {
            String label = TAG.matcher(match.group(2)).replaceAll("").strip();
            String address = match.group(1).strip();
            return Matcher.quoteReplacement(label.isEmpty() || label.equals(address) ? address : label + " (" + address + ")");
        });
        text = BREAK.matcher(text).replaceAll("\n");
        text = TAG.matcher(text).replaceAll("");
        text = ENTITY.matcher(text).replaceAll(match -> Matcher.quoteReplacement(decode(match.group(1))));
        return text.replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n").strip();
    }

    private static String decode(String entity) {
        if (entity.startsWith("#x") || entity.startsWith("#X")) {
            return Character.toString(Integer.parseInt(entity.substring(2), 16));
        }
        if (entity.startsWith("#")) {
            return Character.toString(Integer.parseInt(entity.substring(1)));
        }
        return NAMED.getOrDefault(entity.toLowerCase(), "&" + entity + ";");
    }
}
