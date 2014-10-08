/** XHtmlTreeBuilder.java.

	Purpose:
		
	Description:
		
	History:
		5:15:37 PM Sep 24, 2014, Created by jumperchen

Copyright (C) 2014 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zsoup.parser;

import org.zkoss.zsoup.nodes.Comment;
import org.zkoss.zsoup.nodes.Document;
import org.zkoss.zsoup.nodes.Node;
import org.zkoss.zsoup.nodes.XmlDeclaration;

/**
 * A XHTML tree builder to support {@link XmlDeclaration} 
 * @author jumperchen
 *
 */
public class XHtmlTreeBuilder extends HtmlTreeBuilder {
	
	public class ExceptionInfo extends RuntimeException {
		public ExceptionInfo(Exception e) {
			super(e);
		}
		public Document getCurrentDocument() {
			return doc;
		}
	}

    protected void runParser() {
        try {
	        super.runParser();
        } catch (Exception e) {
        	throw new ExceptionInfo(e); 
        }
    }

    void insert(Token.Comment commentToken) {
        Comment comment = new Comment(commentToken.getData(), baseUri);
        Node insert = comment;
        if (commentToken.bogus) { // xml declarations are emitted as bogus comments (which is right for html, but not xml)
            String data = comment.getData();
            if (data.length() > 1 && (data.startsWith("!") || data.startsWith("?"))) {
                String declaration = data.substring(1);
                insert = new XmlDeclaration(declaration, comment.baseUri(), data.startsWith("!"));
            }
        }
        insertNode(insert);
    }
}
