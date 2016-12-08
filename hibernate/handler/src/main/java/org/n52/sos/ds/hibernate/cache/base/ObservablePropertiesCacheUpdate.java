/*
 * Copyright (C) 2012-2016 52°North Initiative for Geospatial Open Source
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
package org.n52.sos.ds.hibernate.cache.base;

import java.util.List;

import org.n52.io.request.IoParameters;
import org.n52.io.request.RequestSimpleParameterSet;
import org.n52.proxy.db.dao.ProxyDatasetDao;
import org.n52.proxy.db.dao.ProxyPhenomenonDao;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.shetland.ogc.ows.exception.NoApplicableCodeException;
import org.n52.sos.ds.hibernate.cache.AbstractThreadableDatasourceCacheUpdate;
import org.n52.sos.ds.hibernate.cache.DatasourceCacheUpdateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christian Autermann <c.autermann@52north.org>
 *
 * @since 4.0.0
 */
public class ObservablePropertiesCacheUpdate extends AbstractThreadableDatasourceCacheUpdate {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObservablePropertiesCacheUpdate.class);

    @Override
    public void execute() {
        LOGGER.debug("Executing ObservablePropertiesCacheUpdate");
        startStopwatch();
        try {
            List<PhenomenonEntity> observableProperties =
                    new ProxyPhenomenonDao(getSession()).getAllInstances(new DbQuery(IoParameters.createDefaults()));
            for (PhenomenonEntity observableProperty : observableProperties) {
                String identifier = observableProperty.getDomainId();
                if (observableProperty.isSetName()) {
                    getCache().addObservablePropertyIdentifierHumanReadableName(identifier,
                            observableProperty.getName());
                }
                if (observableProperty.hasChilds()) {
                    for (PhenomenonEntity child : observableProperty.getChilds()) {
                        getCache().addCompositePhenomenonForObservableProperty(child.getDomainId(), identifier);
                        getCache().addObservablePropertyForCompositePhenomenon(identifier, child.getDomainId());
                    }
                }

                List<DatasetEntity> datasets = new ProxyDatasetDao<>(getSession()).getAllInstances(createDatasetDbQuery(observableProperty));

                if (datasets != null && !datasets.isEmpty()) {
                    getCache().setOfferingsForObservableProperty(identifier,
                            DatasourceCacheUpdateHelper
                                    .getAllOfferingIdentifiersFromDatasets(datasets));
                    getCache().setProceduresForObservableProperty(identifier,
                            DatasourceCacheUpdateHelper
                                    .getAllProcedureIdentifiersFromDatasets(datasets));
                }
            }
        } catch (DataAccessException dae) {
            getErrors().add(new NoApplicableCodeException().causedBy(dae)
                    .withMessage("Error while updating featureOfInterest cache!"));
        }
        LOGGER.debug("Executing ObservablePropertiesCacheUpdate ({})", getStopwatchResult());
    }

    private DbQuery createDatasetDbQuery(PhenomenonEntity observableProperty) {
        RequestSimpleParameterSet rsps = new RequestSimpleParameterSet();
        rsps.addParameter(IoParameters.PHENOMENA, IoParameters.getJsonNodeFrom(observableProperty.getPkid()));
        return new DbQuery(IoParameters.createFromQuery(rsps));
    }
}
