/*
 * Copyright (C) 2012-2017 52°North Initiative for Geospatial Open Source
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
package org.n52.sos.ds;

import static org.n52.janmayen.http.HTTPStatus.INTERNAL_SERVER_ERROR;

import java.util.Set;

import javax.inject.Inject;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.n52.iceland.i18n.I18NDAORepository;
import org.n52.iceland.ogc.ows.OwsServiceMetadataRepository;
import org.n52.iceland.util.LocalizedProducer;
import org.n52.io.request.IoParameters;
import org.n52.io.request.RequestSimpleParameterSet;
import org.n52.janmayen.i18n.LocaleHelper;
import org.n52.janmayen.lifecycle.Constructable;
import org.n52.proxy.db.dao.ProxyProcedureDao;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.HibernateSessionStore;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.shetland.ogc.ows.OwsAnyValue;
import org.n52.shetland.ogc.ows.OwsDomain;
import org.n52.shetland.ogc.ows.OwsServiceProvider;
import org.n52.shetland.ogc.ows.exception.NoApplicableCodeException;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.sos.Sos2Constants;
import org.n52.shetland.ogc.sos.SosConstants;
import org.n52.shetland.ogc.sos.SosProcedureDescription;
import org.n52.shetland.ogc.sos.request.DescribeSensorRequest;
import org.n52.shetland.ogc.sos.response.DescribeSensorResponse;
import org.n52.sos.ds.dao.DescribeSensorDao;
import org.n52.sos.ds.procedure.ProcedureConverter;
import org.springframework.beans.factory.annotation.Autowired;

public class DescribeSensorHandler extends AbstractDescribeSensorHandler implements Constructable {

    private HibernateSessionStore sessionStore;
    private DescribeSensorDao dao;

    private OwsServiceMetadataRepository serviceMetadataRepository;
    private ProcedureConverter procedureConverter;
    private I18NDAORepository i18NDAORepository;

    public DescribeSensorHandler() {
        super(SosConstants.SOS);
    }

    @Override
    protected Set<OwsDomain> getOperationParameters(String service, String version) {
        Set<OwsDomain> operationParameters = super.getOperationParameters(service, version);
        if (version.equals(Sos2Constants.SERVICEVERSION)) {
            operationParameters.add(new OwsDomain(Sos2Constants.DescribeSensorParams.validTime, OwsAnyValue.instance()));
        }
        return operationParameters;
    }

    @Autowired(required=false)
    public void setDescribeSensorDao(DescribeSensorDao describeSensorDao) {
        this.dao = describeSensorDao;
    }

    @Inject
    public void setServiceMetadataRepository(OwsServiceMetadataRepository repo) {
        this.serviceMetadataRepository = repo;
    }

    @Inject
    public void setConnectionProvider(HibernateSessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Inject
    public void setI18NDAORepository(I18NDAORepository i18NDAORepository) {
        this.i18NDAORepository = i18NDAORepository;
    }

    @Override
    public void init() {
        LocalizedProducer<OwsServiceProvider> serviceProvider
                = this.serviceMetadataRepository.getServiceProviderFactory(SosConstants.SOS);
        this.procedureConverter = new ProcedureConverter(serviceProvider);
    }

    @Override
    public DescribeSensorResponse getSensorDescription(final DescribeSensorRequest request)
            throws OwsExceptionReport {
        Session session = null;
        try {
            session = sessionStore.getSession();
            final DescribeSensorResponse response = new DescribeSensorResponse();
            response.setService(request.getService());
            response.setVersion(request.getVersion());
            response.setOutputFormat(request.getProcedureDescriptionFormat());

            ProcedureEntity entity = new ProxyProcedureDao(session).getInstance(request.getProcedure(), createDbQuery(request));
            if (entity == null) {
                throw new NoApplicableCodeException().causedBy(
                        new IllegalArgumentException("Parameter 'procedure' should not be null!")).setStatus(
                                INTERNAL_SERVER_ERROR);
            }
            if (dao != null) {
                response.setSensorDescriptions(dao.querySensorDescriptions(request));
            } else {
                response.addSensorDescription(createSensorDescription(entity, request, session));
            }
            return response;
        } catch (final HibernateException | DataAccessException e) {
            throw new NoApplicableCodeException().causedBy(e).withMessage(
                    "Error while querying data for DescribeSensor document!");
        } finally {
            sessionStore.returnSession(session);
        }
    }

    private SosProcedureDescription<?> createSensorDescription(ProcedureEntity procedure, DescribeSensorRequest request, Session session) throws OwsExceptionReport {
        return procedureConverter.createSosProcedureDescription(procedure,
                request.getProcedureDescriptionFormat(),
                request.getVersion(),
                LocaleHelper.decode(request.getRequestedLanguage()),
                i18NDAORepository,
                session);
    }

    private DbQuery createDbQuery(DescribeSensorRequest req) {
        RequestSimpleParameterSet rsps = new RequestSimpleParameterSet();
        if (req.isSetProcedure()) {
            rsps.addParameter(IoParameters.PROCEDURES, IoParameters.getJsonNodeFrom(req.getProcedure()));
        }
        rsps.addParameter(IoParameters.MATCH_DOMAIN_IDS, IoParameters.getJsonNodeFrom(true));
        return new DbQuery(IoParameters.createFromQuery(rsps));
    }
}
