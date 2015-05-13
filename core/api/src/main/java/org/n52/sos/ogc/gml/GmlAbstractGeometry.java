/**
 * Copyright (C) 2012-2015 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.sos.ogc.gml;

import org.n52.iceland.ogc.gml.AbstractFeature;
import org.n52.iceland.ogc.gml.GmlConstants;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A class that represents a gml:AbstractGeometry (PointType, ...).
 * 
 * @since 4.0.0
 * 
 */
public class GmlAbstractGeometry extends AbstractFeature {

    /**
     * serial number
     */
    private static final long serialVersionUID = -2997129475905560236L;

    /**
     * Geometry
     */
    private Geometry geometry;

    /**
     * constructor
     */
    public GmlAbstractGeometry() {
    }

    /**
     * constructor
     * 
     * @param id
     *            GML id
     */
    public GmlAbstractGeometry(String id) {
        setGmlId(id);
    }

    /**
     * Get geometry
     * 
     * @return the geometry
     */
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * set geometry
     * 
     * @param geometry
     *            the geometry to set
     */
    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    /**
     * Is geometry set
     * 
     * @return true if geometry is set
     */
    public boolean isSetGeometry() {
        return this.geometry != null;
    }

	@Override
	public String getDefaultElementEncoding() {
		return GmlConstants.NS_GML_32;
	}
}
