package com.example.book2quiz.service;

import java.util.List;

/**
 * Builds a single XML section wrapping a list of items:
 * {@code <tag><childTag>item</childTag>...</tag>}.
 *
 * <p>Empty (or all-blank) sections render as an empty string so callers can omit them.
 * Item content is sanitized so it cannot break out of its tag: any {@code "</"} is
 * escaped to {@code "<\\/"}, which no XML parser or the model will read as a closing tag.
 */
public final class XmlSection {

    private XmlSection() {
    }

    public static String of(String tag, String childTag, List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean anyWritten = false;
        for (String item : items) {
            if (item == null || item.isBlank()) {
                continue;
            }
            sb.append("  <").append(childTag).append('>')
                    .append(sanitize(item.strip()))
                    .append("</").append(childTag).append(">\n");
            anyWritten = true;
        }

        if (!anyWritten) {
            return "";
        }
        return "<" + tag + ">\n" + sb + "</" + tag + ">";
    }

    private static String sanitize(String content) {
        // Neutralize any closing-tag sequence so item content can't escape its element.
        return content.replace("</", "<\\/");
    }
}
