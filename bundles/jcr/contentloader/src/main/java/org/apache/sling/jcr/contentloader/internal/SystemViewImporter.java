/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jcr.contentloader.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;

public class SystemViewImporter {

    public static final String EXT_JCR_XML = ".jcr.xml";

    private final Logger logger = LoggerFactory.getLogger(SystemViewImporter.class);

    public SystemViewImporter() {
    }

    // TODO from Loader

    /**
     * Import the XML file as JCR system or document view import. If the XML
     * file is not a valid system or document view export/import file,
     * <code>false</code> is returned.
     *
     * @param parent  The parent node below which to import
     * @param nodeXML The URL to the XML file to import
     * @return <code>true</code> if the import succeeds, <code>false</code>
     *         if the import fails due to XML format errors.
     * @throws java.io.IOException If an IO error occurs reading the XML file.
     */
    protected Node importSystemView(Node parent, String name, URL nodeXML) throws IOException {

        InputStream ins = null;
        try {
            // check whether we have the content already, nothing to do then
            if (name.endsWith(EXT_JCR_XML)) {
                name = name.substring(0, name.length() - EXT_JCR_XML.length());
            }
            if (parent.hasNode(name)) {
                logger.debug("importSystemView: Node {} for XML {} already exists, nothing to do", name, nodeXML);
                return parent.getNode(name);
            }

            ins = nodeXML.openStream();
            Session session = parent.getSession();
            session.importXML(parent.getPath(), ins, IMPORT_UUID_CREATE_NEW);

            // additionally check whether the expected child node exists
            return (parent.hasNode(name)) ? parent.getNode(name) : null;
        } catch (InvalidSerializedDataException isde) {
            // the XML might not be system or document view export, fall back
            // to old-style XML reading
            logger.info("importSystemView: XML {} does not seem to be system view export, trying old style; cause: {}", nodeXML, isde.toString());
            return null;
        } catch (RepositoryException re) {
            // any other repository related issue...
            logger.info("importSystemView: Repository issue loading XML {}, trying old style; cause: {}", nodeXML, re.toString());
            return null;
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

    // TODO from DefaultContentImporter

    /**
     * Import the XML file as JCR system or document view import. If the XML
     * file is not a valid system or document view export/import file,
     * <code>false</code> is returned.
     *
     * @param parent        The parent node below which to import
     * @param name          the name of the import resource
     * @param contentStream The XML content to import
     * @param replace       Whether or not to replace the subtree at name if the
     *                      node exists.
     * @return <code>true</code> if the import succeeds, <code>false</code> if the import fails due to XML format errors.
     * @throws IOException If an IO error occurrs reading the XML file.
     */
    protected Node importSystemView(Node parent, String name, InputStream contentStream, boolean replace) throws IOException {
        InputStream ins = null;
        try {
            // check whether we have the content already, nothing to do then
            final String nodeName = (name.endsWith(EXT_JCR_XML)) ? name.substring(0, name.length() - EXT_JCR_XML.length()) : name;

            // ensure the name is not empty
            if (nodeName.length() == 0) {
                throw new IOException("Node name must not be empty (or extension only)");
            }

            // check for existence/replacement
            if (parent.hasNode(nodeName)) {
                Node existingNode = parent.getNode(nodeName);
                if (replace) {
                    logger.debug("importSystemView: Removing existing node at {}", nodeName);
                    existingNode.remove();
                } else {
                    logger.debug("importSystemView: Node {} for XML already exists, nothing to to", nodeName);
                    return existingNode;
                }
            }

            final int uuidBehavior;
            if (replace) {
                uuidBehavior = IMPORT_UUID_COLLISION_REPLACE_EXISTING;
            } else {
                uuidBehavior = IMPORT_UUID_CREATE_NEW;
            }

            ins = contentStream;
            Session session = parent.getSession();
            session.importXML(parent.getPath(), ins, uuidBehavior);

            // additionally check whether the expected child node exists
            return (parent.hasNode(nodeName)) ? parent.getNode(nodeName) : null;
        } catch (InvalidSerializedDataException isde) {
            // the xml might not be System or Document View export, fall back to old-style XML reading
            logger.info("importSystemView: XML does not seem to be system view export; cause: {}", isde.toString());
            return null;
        } catch (RepositoryException re) {
            // any other repository related issue...
            logger.info("importSystemView: Repository issue loading XML; cause: {}", re.toString());
            return null;
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

}