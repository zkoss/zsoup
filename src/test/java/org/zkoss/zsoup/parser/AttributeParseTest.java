package org.zkoss.zsoup.parser;

import org.junit.Test;
import org.zkoss.zsoup.Zsoup;
import org.zkoss.zsoup.nodes.Attributes;
import org.zkoss.zsoup.nodes.Element;
import org.zkoss.zsoup.select.Elements;

import static org.junit.Assert.*;

/**
 Test suite for attribute parser.

 @author Jonathan Hedley, jonathan@hedley.net */
public class AttributeParseTest {

    @Test public void parsesRoughAttributeString() {
        String html = "<a id=\"123\" class=\"baz = 'bar'\" style = 'border: 2px'qux zim foo = 12 mux=18 />";
        // should be: <id=123>, <class=baz = 'bar'>, <qux=>, <zim=>, <foo=12>, <mux.=18>

        Element el = Zsoup.parse(html).getElementsByTag("a").get(0);
        Attributes attr = el.attributes();
        assertEquals(7, attr.size());
        assertEquals("123", attr.get("id"));
        assertEquals("baz = 'bar'", attr.get("class"));
        assertEquals("border: 2px", attr.get("style"));
        assertEquals("", attr.get("qux"));
        assertEquals("", attr.get("zim"));
        assertEquals("12", attr.get("foo"));
        assertEquals("18", attr.get("mux"));
    }

    @Test public void handlesNewLinesAndReturns() {
        String html = "<a\r\nfoo='bar\r\nqux'\r\nbar\r\n=\r\ntwo>One</a>";
        Element el = Zsoup.parse(html).select("a").first();
        assertEquals(2, el.attributes().size());
        assertEquals("bar\r\nqux", el.attr("foo")); // currently preserves newlines in quoted attributes. todo confirm if should.
        assertEquals("two", el.attr("bar"));
    }

    @Test public void parsesEmptyString() {
        String html = "<a />";
        Element el = Zsoup.parse(html).getElementsByTag("a").get(0);
        Attributes attr = el.attributes();
        assertEquals(0, attr.size());
    }

    @Test public void canStartWithEq() {
        String html = "<a =empty />";
        Element el = Zsoup.parse(html).getElementsByTag("a").get(0);
        Attributes attr = el.attributes();
        assertEquals(1, attr.size());
        assertTrue(attr.hasKey("=empty"));
        assertEquals("", attr.get("=empty"));
    }

    @Test public void strictAttributeUnescapes() {
        String html = "<a id=1 href='?foo=bar&mid&lt=true'>One</a> <a id=2 href='?foo=bar&lt;qux&lg=1'>Two</a>";
        Elements els = Zsoup.parse(html).select("a");
        assertEquals("?foo=bar&mid&lt=true", els.first().attr("href"));
        assertEquals("?foo=bar<qux&lg=1", els.last().attr("href"));
    }

    @Test public void moreAttributeUnescapes() {
        String html = "<a href='&wr_id=123&mid-size=true&ok=&wr'>Check</a>";
        Elements els = Zsoup.parse(html).select("a");
        assertEquals("&wr_id=123&mid-size=true&ok=&wr", els.first().attr("href"));
    }
}
