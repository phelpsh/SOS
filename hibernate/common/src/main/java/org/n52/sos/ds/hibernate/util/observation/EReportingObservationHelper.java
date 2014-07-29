/**
 * Copyright (C) 2012-2014 52°North Initiative for Geospatial Open Source
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
package org.n52.sos.ds.hibernate.util.observation;

import java.util.Collection;







import org.n52.sos.aqd.AqdConstants.AssesmentType;
import org.n52.sos.aqd.AqdConstants.ProcessParameter;
import org.n52.sos.ds.hibernate.entities.ereporting.EReportingSeries;
import org.n52.sos.ogc.gml.ReferenceType;
import org.n52.sos.ogc.om.NamedValue;
import org.n52.sos.ogc.om.values.ReferenceValue;

import com.google.common.collect.Lists;

public class EReportingObservationHelper {
    
    public Collection<NamedValue<?>> createSamplingPointParameter(EReportingSeries series) {
        Collection<NamedValue<?>> namedValues = Lists.newArrayListWithCapacity(2);
        namedValues.add(getAssessmentType());
        namedValues.add(getAssesmentMethod(series));
        return namedValues;
    }

    private NamedValue<?> getAssessmentType() {
        NamedValue<ReferenceType> namedValue = new NamedValue<ReferenceType>();
        namedValue.setName(new ReferenceType(ProcessParameter.AssesmentType.getConceptURI()));
        namedValue.setValue(createReferenceValue(AssesmentType.Fixed.getConceptURI()));
        return namedValue;
    }

    private NamedValue<?> getAssesmentMethod(EReportingSeries series) {
        NamedValue<ReferenceType> namedValue = new NamedValue<ReferenceType>();
        namedValue.setName(new ReferenceType(ProcessParameter.SamplingPoint.getConceptURI()));
        namedValue.setValue(createReferenceValue(series.getSamplingPoint().getIdentifier()));
        return namedValue;
    }
    
    private ReferenceValue createReferenceValue(String value) {
        ReferenceValue referenceValue = new ReferenceValue();
        referenceValue.setValue(new ReferenceType(value));
        return referenceValue;
    }

}
