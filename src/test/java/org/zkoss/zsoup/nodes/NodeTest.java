package org.zkoss.zsoup.nodes;

import org.junit.Test;
import org.zkoss.zsoup.Zsoup;
import org.zkoss.zsoup.TextUtil;
import org.zkoss.zsoup.nodes.Attributes;
import org.zkoss.zsoup.nodes.Document;
import org.zkoss.zsoup.nodes.Element;
import org.zkoss.zsoup.nodes.Node;
import org.zkoss.zsoup.nodes.TextNode;
import org.zkoss.zsoup.parser.Tag;
import org.zkoss.zsoup.select.Elements;
import org.zkoss.zsoup.select.NodeVisitor;

import java.util.List;

import static org.junit.Assert.*;
/**
 Tests Nodes

 @author Jonathan Hedley, jonathan@hedley.net */
public class NodeTest {
    @Test public void handlesBaseUri() {
        Tag tag = Tag.valueOf("a");
        Attributes attribs = new Attributes();
        attribs.put("relHref", "/foo");
        attribs.put("absHref", "http://bar/qux");

        Element noBase = new Element(tag, "", attribs);
        assertEquals("", noBase.absUrl("relHref")); // with no base, should NOT fallback to href attrib, whatever it is
        assertEquals("http://bar/qux", noBase.absUrl("absHref")); // no base but valid attrib, return attrib

        Element withBase = new Element(tag, "http://foo/", attribs);
        assertEquals("http://foo/foo", withBase.absUrl("relHref")); // construct abs from base + rel
        assertEquals("http://bar/qux", withBase.absUrl("absHref")); // href is abs, so returns that
        assertEquals("", withBase.absUrl("noval"));

        Element dodgyBase = new Element(tag, "wtf://no-such-protocol/", attribs);
        assertEquals("http://bar/qux", dodgyBase.absUrl("absHref")); // base fails, but href good, so get that
        assertEquals("", dodgyBase.absUrl("relHref")); // base fails, only rel href, so return nothing 
    }

    @Test public void setBaseUriIsRecursive() {
        Document doc = Zsoup.parse("<div><p></p></div>");
        String baseUri = "http://jsoup.org";
        doc.setBaseUri(baseUri);
        
        assertEquals(baseUri, doc.baseUri());
        assertEquals(baseUri, doc.select("div").first().baseUri());
        assertEquals(baseUri, doc.select("p").first().baseUri());
    }

    @Test public void handlesAbsPrefix() {
        Document doc = Zsoup.parse("<a href=/foo>Hello</a>", "http://jsoup.org/");
        Element a = doc.select("a").first();
        assertEquals("/foo", a.attr("href"));
        assertEquals("http://jsoup.org/foo", a.attr("abs:href"));
        assertTrue(a.hasAttr("abs:href"));
    }

    @Test public void handlesAbsOnImage() {
        Document doc = Zsoup.parse("<p><img src=\"/rez/osi_logo.png\" /></p>", "http://jsoup.org/");
        Element img = doc.select("img").first();
        assertEquals("http://jsoup.org/rez/osi_logo.png", img.attr("abs:src"));
        assertEquals(img.absUrl("src"), img.attr("abs:src"));
    }

    @Test public void handlesAbsPrefixOnHasAttr() {
        // 1: no abs url; 2: has abs url
        Document doc = Zsoup.parse("<a id=1 href='/foo'>One</a> <a id=2 href='http://jsoup.org/'>Two</a>");
        Element one = doc.select("#1").first();
        Element two = doc.select("#2").first();

        assertFalse(one.hasAttr("abs:href"));
        assertTrue(one.hasAttr("href"));
        assertEquals("", one.absUrl("href"));

        assertTrue(two.hasAttr("abs:href"));
        assertTrue(two.hasAttr("href"));
        assertEquals("http://jsoup.org/", two.absUrl("href"));
    }

    @Test public void literalAbsPrefix() {
        // if there is a literal attribute "abs:xxx", don't try and make absolute.
        Document doc = Zsoup.parse("<a abs:href='odd'>One</a>");
        Element el = doc.select("a").first();
        assertTrue(el.hasAttr("abs:href"));
        assertEquals("odd", el.attr("abs:href"));
    }

    @Test public void handleAbsOnFileUris() {
        Document doc = Zsoup.parse("<a href='password'>One/a><a href='/var/log/messages'>Two</a>", "file:/etc/");
        Element one = doc.select("a").first();
        assertEquals("file:/etc/password", one.absUrl("href"));
        Element two = doc.select("a").get(1);
        assertEquals("file:/var/log/messages", two.absUrl("href"));
    }

    @Test
    public void handleAbsOnLocalhostFileUris() {
        Document doc = Zsoup.parse("<a href='password'>One/a><a href='/var/log/messages'>Two</a>", "file://localhost/etc/");
        Element one = doc.select("a").first();
        assertEquals("file://localhost/etc/password", one.absUrl("href"));
    }

    @Test
    public void handlesAbsOnProtocolessAbsoluteUris() {
        Document doc1 = Zsoup.parse("<a href='//example.net/foo'>One</a>", "http://example.com/");
        Document doc2 = Zsoup.parse("<a href='//example.net/foo'>One</a>", "https://example.com/");

        Element one = doc1.select("a").first();
        Element two = doc2.select("a").first();

        assertEquals("http://example.net/foo", one.absUrl("href"));
        assertEquals("https://example.net/foo", two.absUrl("href"));

        Document doc3 = Zsoup.parse("<img src=//www.google.com/images/errors/logo_sm.gif alt=Google>", "https://google.com");
        assertEquals("https://www.google.com/images/errors/logo_sm.gif", doc3.select("img").attr("abs:src"));
    }

    /*
    Test for an issue with Java's abs URL handler.
     */
    @Test public void absHandlesRelativeQuery() {
        Document doc = Zsoup.parse("<a href='?foo'>One</a> <a href='bar.html?foo'>Two</a>", "http://jsoup.org/path/file?bar");

        Element a1 = doc.select("a").first();
        assertEquals("http://jsoup.org/path/file?foo", a1.absUrl("href"));

        Element a2 = doc.select("a").get(1);
        assertEquals("http://jsoup.org/path/bar.html?foo", a2.absUrl("href"));
    }
    
    @Test public void testRemove() {
        Document doc = Zsoup.parse("<p>One <span>two</span> three</p>");
        Element p = doc.select("p").first();
        p.childNode(0).remove();
        
        assertEquals("two three", p.text());
        assertEquals("<span>two</span> three", TextUtil.stripNewlines(p.html()));
    }
    
    @Test public void testReplace() {
        Document doc = Zsoup.parse("<p>One <span>two</span> three</p>");
        Element p = doc.select("p").first();
        Element insert = doc.createElement("em").text("foo");
        p.childNode(1).replaceWith(insert);
        
        assertEquals("One <em>foo</em> three", p.html());
    }
    
    @Test public void ownerDocument() {
        Document doc = Zsoup.parse("<p>Hello");
        Element p = doc.select("p").first();
        assertTrue(p.ownerDocument() == doc);
        assertTrue(doc.ownerDocument() == doc);
        assertNull(doc.parent());
    }

    @Test public void before() {
        Document doc = Zsoup.parse("<p>One <b>two</b> three</p>");
        Element newNode = new Element(Tag.valueOf("em"), "");
        newNode.appendText("four");

        doc.select("b").first().before(newNode);
        assertEquals("<p>One <em>four</em><b>two</b> three</p>", doc.body().html());

        doc.select("b").first().before("<i>five</i>");
        assertEquals("<p>One <em>four</em><i>five</i><b>two</b> three</p>", doc.body().html());
    }

    @Test public void after() {
        Document doc = Zsoup.parse("<p>One <b>two</b> three</p>");
        Element newNode = new Element(Tag.valueOf("em"), "");
        newNode.appendText("four");

        doc.select("b").first().after(newNode);
        assertEquals("<p>One <b>two</b><em>four</em> three</p>", doc.body().html());

        doc.select("b").first().after("<i>five</i>");
        assertEquals("<p>One <b>two</b><i>five</i><em>four</em> three</p>", doc.body().html());
    }

    @Test public void unwrap() {
        Document doc = Zsoup.parse("<div>One <span>Two <b>Three</b></span> Four</div>");
        Element span = doc.select("span").first();
        Node twoText = span.childNode(0);
        Node node = span.unwrap();

        assertEquals("<div>One Two <b>Three</b> Four</div>", TextUtil.stripNewlines(doc.body().html()));
        assertTrue(node instanceof TextNode);
        assertEquals("Two ", ((TextNode) node).text());
        assertEquals(node, twoText);
        assertEquals(node.parent(), doc.select("div").first());
    }

    @Test public void unwrapNoChildren() {
        Document doc = Zsoup.parse("<div>One <span></span> Two</div>");
        Element span = doc.select("span").first();
        Node node = span.unwrap();
        assertEquals("<div>One  Two</div>", TextUtil.stripNewlines(doc.body().html()));
        assertTrue(node == null);
    }

    @Test public void traverse() {
        Document doc = Zsoup.parse("<div><p>Hello</p></div><div>There</div>");
        final StringBuilder accum = new StringBuilder();
        doc.select("div").first().traverse(new NodeVisitor() {
            public void head(Node node, int depth) {
                accum.append("<" + node.nodeName() + ">");
            }

            public void tail(Node node, int depth) {
                accum.append("</" + node.nodeName() + ">");
            }
        });
        assertEquals("<div><p><#text></#text></p></div>", accum.toString());
    }

    @Test public void orphanNodeReturnsNullForSiblingElements() {
        Node node = new Element(Tag.valueOf("p"), "");
        Element el = new Element(Tag.valueOf("p"), "");

        assertEquals(0, node.siblingIndex());
        assertEquals(0, node.siblingNodes().size());

        assertNull(node.previousSibling());
        assertNull(node.nextSibling());

        assertEquals(0, el.siblingElements().size());
        assertNull(el.previousElementSibling());
        assertNull(el.nextElementSibling());
    }

    @Test public void nodeIsNotASiblingOfItself() {
        Document doc = Zsoup.parse("<div><p>One<p>Two<p>Three</div>");
        Element p2 = doc.select("p").get(1);

        assertEquals("Two", p2.text());
        List<Node> nodes = p2.siblingNodes();
        assertEquals(2, nodes.size());
        assertEquals("<p>One</p>", nodes.get(0).outerHtml());
        assertEquals("<p>Three</p>", nodes.get(1).outerHtml());
    }

    @Test public void childNodesCopy() {
        Document doc = Zsoup.parse("<div id=1>Text 1 <p>One</p> Text 2 <p>Two<p>Three</div><div id=2>");
        Element div1 = doc.select("#1").first();
        Element div2 = doc.select("#2").first();
        List<Node> divChildren = div1.childNodesCopy();
        assertEquals(5, divChildren.size());
        TextNode tn1 = (TextNode) div1.childNode(0);
        TextNode tn2 = (TextNode) divChildren.get(0);
        tn2.text("Text 1 updated");
        assertEquals("Text 1 ", tn1.text());
        div2.insertChildren(-1, divChildren);
        assertEquals("<div id=\"1\">Text 1 <p>One</p> Text 2 <p>Two</p><p>Three</p></div><div id=\"2\">Text 1 updated"
            +"<p>One</p> Text 2 <p>Two</p><p>Three</p></div>", TextUtil.stripNewlines(doc.body().html()));
    }
}