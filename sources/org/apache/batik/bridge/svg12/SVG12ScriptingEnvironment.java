package org.apache.batik.bridge.svg12;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.Messages;
import org.apache.batik.bridge.ScriptingEnvironment;
import org.apache.batik.bridge.SVGUtilities;
import org.apache.batik.dom.AbstractDocument;
import org.apache.batik.dom.AbstractElement;
import org.apache.batik.dom.util.DOMUtilities;
import org.apache.batik.dom.util.TriplyIndexedTable;
import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.SVG12Constants;
import org.apache.batik.util.XMLConstants;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;

/**
 * Manages scripting handlers for SVG 1.2 'handler' elements.
 *
 * @author <a href="mailto:cam%40mcc%2eid%2eau">Cameron McCormack</a>
 * @version $Id$
 */
public class SVG12ScriptingEnvironment extends ScriptingEnvironment {

    /**
     * Constant used to describe handler scripts.
     * {0} - URL of document containing script.
     * {1} - Event type
     * {2} - Event namespace
     * {3} - line number of element.
     */
    public static final String HANDLER_SCRIPT_DESCRIPTION
        = "SVG12ScriptingEnvironment.constant.handler.script.description";

    /**
     * Creates a new SVG12ScriptingEnvironment.
     * @param ctx the bridge context
     */
    public SVG12ScriptingEnvironment(BridgeContext ctx) {
        super(ctx);
    }

    /**
     * The listeners for XML Events style handlers.
     * Maps (event namespace, event local name, element) to a handler.
     */
    protected TriplyIndexedTable handlerScriptingListeners;

    /**
     * Adds the scripting listeners to the given element.
     */
    protected void addScriptingListenersOn(Element elt) {
        String eltNS = elt.getNamespaceURI();
        String eltLN = elt.getLocalName();
        if (SVGConstants.SVG_NAMESPACE_URI.equals(eltNS)
                && SVG12Constants.SVG_HANDLER_TAG.equals(eltLN)) {
            // For this 'handler' element, add a handler for the given
            // event type.
            AbstractElement tgt = (AbstractElement) elt.getParentNode();
            String eventType = elt.getAttributeNS
                (XMLConstants.XML_EVENTS_NAMESPACE_URI,
                 XMLConstants.XML_EVENTS_EVENT_ATTRIBUTE);
            String eventNamespaceURI = XMLConstants.XML_EVENTS_NAMESPACE_URI;
            if (eventType.indexOf(':') != -1) {
                String prefix = DOMUtilities.getPrefix(eventType);
                eventType = DOMUtilities.getLocalName(eventType);
                eventNamespaceURI
                    = ((AbstractElement) elt).lookupNamespaceURI(prefix);
            }

            EventListener listener = new HandlerScriptingEventListener
                (eventNamespaceURI, eventType, (AbstractElement) elt);
            tgt.addEventListenerNS
                (eventNamespaceURI, eventType, listener, false, null);
            if (handlerScriptingListeners == null) {
                handlerScriptingListeners = new TriplyIndexedTable();
            }
            handlerScriptingListeners.put
                (eventNamespaceURI, eventType, elt, listener);
        }

        super.addScriptingListenersOn(elt);
    }

    /**
     * Removes the scripting listeners from the given element.
     */
    protected void removeScriptingListenersOn(Element elt) {
        String eltNS = elt.getNamespaceURI();
        String eltLN = elt.getLocalName();
        if (SVGConstants.SVG_NAMESPACE_URI.equals(eltNS)
                && SVG12Constants.SVG_HANDLER_TAG.equals(eltLN)) {
            // For this 'handler' element, remove the handler for the given
            // event type.
            AbstractElement tgt = (AbstractElement) elt.getParentNode();
            String eventType = elt.getAttributeNS
                (XMLConstants.XML_EVENTS_NAMESPACE_URI,
                 XMLConstants.XML_EVENTS_EVENT_ATTRIBUTE);
            String eventNamespaceURI = XMLConstants.XML_EVENTS_NAMESPACE_URI;
            if (eventType.indexOf(':') != -1) {
                String prefix = DOMUtilities.getPrefix(eventType);
                eventType = DOMUtilities.getLocalName(eventType);
                eventNamespaceURI
                    = ((AbstractElement) elt).lookupNamespaceURI(prefix);
            }

            EventListener listener =
                (EventListener) handlerScriptingListeners.put
                    (eventNamespaceURI, eventType, elt, null);
            tgt.removeEventListenerNS
                (eventNamespaceURI, eventType, listener, false);
        }

        super.removeScriptingListenersOn(elt);
    }

    /**
     * To handle a scripting event with an XML Events style handler.
     */
    protected class HandlerScriptingEventListener implements EventListener {

        /**
         * The namespace URI of the event type.
         */
        protected String eventNamespaceURI;

        /**
         * The event type.
         */
        protected String eventType;

        /**
         * The handler element.
         */
        protected AbstractElement handlerElement;

        /**
         * Creates a new HandlerScriptingEventListener.
         * @param ns Namespace URI of the event type.
         * @param et The event type.
         * @param e The handler element.
         */
        public HandlerScriptingEventListener(String ns,
                                             String et,
                                             AbstractElement e) {
            eventNamespaceURI = ns;
            eventType = et;
            handlerElement = e;
        }

        /**
         * Runs the script.
         */
        public void handleEvent(Event evt) {
            Element elt = (Element)evt.getCurrentTarget();
            // Evaluate the script
            String script = handlerElement.getTextContent();
            if (script.length() == 0)
                return;

            DocumentLoader dl = bridgeContext.getDocumentLoader();
            AbstractDocument d
                = (AbstractDocument) handlerElement.getOwnerDocument();
            int line = dl.getLineNumber(handlerElement);
            final String desc = Messages.formatMessage
                (HANDLER_SCRIPT_DESCRIPTION,
                 new Object [] {d.getDocumentURI(),
                                eventNamespaceURI,
                                eventType,
                                new Integer(line)});

            // Find the scripting language
            String lang = handlerElement.getAttributeNS
                (null, SVGConstants.SVG_CONTENT_SCRIPT_TYPE_ATTRIBUTE);
            if (lang.length() == 0) {
                Element e = elt;
                while (e != null &&
                       (!SVGConstants.SVG_NAMESPACE_URI.equals
                        (e.getNamespaceURI()) ||
                        !SVGConstants.SVG_SVG_TAG.equals(e.getLocalName()))) {
                    e = SVGUtilities.getParentElement(e);
                }
                if (e == null)
                    return;

                lang = e.getAttributeNS
                    (null, SVGConstants.SVG_CONTENT_SCRIPT_TYPE_ATTRIBUTE);
            }

            runEventHandler(script, evt, lang, desc);
        }
    }
}
