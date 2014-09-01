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
package org.n52.sos.ds.hibernate.dao.observation.series;

import static org.hibernate.criterion.Restrictions.eq;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.sos.ds.hibernate.dao.DaoFactory;
import org.n52.sos.ds.hibernate.dao.observation.AbstractObservationDAO;
import org.n52.sos.ds.hibernate.dao.observation.ObservationContext;
import org.n52.sos.ds.hibernate.entities.FeatureOfInterest;
import org.n52.sos.ds.hibernate.entities.ObservableProperty;
import org.n52.sos.ds.hibernate.entities.Offering;
import org.n52.sos.ds.hibernate.entities.Procedure;
import org.n52.sos.ds.hibernate.entities.observation.AbstractObservation;
import org.n52.sos.ds.hibernate.entities.observation.AbstractTemporalReferencedObservation;
import org.n52.sos.ds.hibernate.entities.observation.Observation;
import org.n52.sos.ds.hibernate.entities.observation.series.AbstractSeriesObservation;
import org.n52.sos.ds.hibernate.entities.observation.series.ContextualReferencedSeriesObservation;
import org.n52.sos.ds.hibernate.entities.observation.series.Series;
import org.n52.sos.ds.hibernate.entities.observation.series.SeriesObservation;
import org.n52.sos.ds.hibernate.entities.observation.series.TemporalReferencedSeriesObservation;
import org.n52.sos.ds.hibernate.util.HibernateHelper;
import org.n52.sos.ds.hibernate.util.ScrollableIterable;
import org.n52.sos.exception.CodedException;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sos.SosConstants.SosIndeterminateTime;
import org.n52.sos.request.GetObservationRequest;
import org.n52.sos.util.CollectionHelper;

import com.vividsolutions.jts.geom.Geometry;

public abstract class AbstractSeriesObservationDAO extends AbstractObservationDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSeriesObservationDAO.class);

    @Override
    protected void addObservationContextToObservation(ObservationContext ctx,
            Observation<?> observation, Session session) throws CodedException {
        AbstractSeriesDAO seriesDAO = DaoFactory.getInstance().getSeriesDAO();
        Series series = seriesDAO.getOrInsertSeries(ctx, session);
        ((SeriesObservation) observation).setSeries(series);
        seriesDAO.updateSeriesWithFirstLatestValues(series, observation, session);
    }

    @Override
    public Criteria getObservationInfoCriteriaForFeatureOfInterestAndProcedure(String feature, String procedure,
            Session session) {
        Criteria criteria = getDefaultObservationInfoCriteria(session);
        Criteria seriesCriteria = criteria.createCriteria(ContextualReferencedSeriesObservation.SERIES);
        seriesCriteria.createCriteria(Series.FEATURE_OF_INTEREST).add(eq(FeatureOfInterest.IDENTIFIER, feature));
        seriesCriteria.createCriteria(Series.PROCEDURE).add(eq(Procedure.IDENTIFIER, procedure));

        if (!isIncludeChildObservableProperties()) {
            seriesCriteria.createCriteria(AbstractObservation.VALUE).createCriteria(AbstractObservation.ID);
        }

        return criteria;
    }

    @Override
    public Criteria getObservationInfoCriteriaForFeatureOfInterestAndOffering(String feature, String offering,
            Session session) {
        Criteria criteria = getDefaultObservationInfoCriteria(session);
        Criteria seriesCriteria = criteria.createCriteria(ContextualReferencedSeriesObservation.SERIES);
        seriesCriteria.createCriteria(Series.FEATURE_OF_INTEREST).add(eq(FeatureOfInterest.IDENTIFIER, feature));
        criteria.createCriteria(AbstractObservation.OFFERINGS).add(eq(Offering.IDENTIFIER, offering));
        return criteria;
    }


    @Override
    public Criteria getObservationCriteriaForProcedure(String procedure, Session session) throws CodedException {
        AbstractSeriesDAO seriesDAO = DaoFactory.getInstance().getSeriesDAO();
        Criteria criteria = getDefaultObservationCriteria(session);
        Criteria seriesCriteria = criteria.createCriteria(AbstractSeriesObservation.SERIES);
        seriesDAO.addProcedureToCriteria(seriesCriteria, procedure);
        return criteria;
    }

    @Override
    public Criteria getObservationCriteriaForObservableProperty(String observableProperty, Session session) throws CodedException {
        AbstractSeriesDAO seriesDAO = DaoFactory.getInstance().getSeriesDAO();
        Criteria criteria = getDefaultObservationCriteria(session);
        Criteria seriesCriteria = criteria.createCriteria(AbstractSeriesObservation.SERIES);
        seriesDAO.addObservablePropertyToCriteria(seriesCriteria, observableProperty);
        return criteria;
    }

    @Override
    public Criteria getObservationCriteriaForFeatureOfInterest(String featureOfInterest, Session session) throws CodedException {
        AbstractSeriesDAO seriesDAO = DaoFactory.getInstance().getSeriesDAO();
        Criteria criteria = getDefaultObservationCriteria(session);
        Criteria seriesCriteria = criteria.createCriteria(AbstractSeriesObservation.SERIES);
        seriesDAO.addFeatureOfInterestToCriteria(seriesCriteria, featureOfInterest);
        return criteria;
    }

    @Override
    public Criteria getObservationCriteriaFor(String procedure, String observableProperty, Session session) throws CodedException {
        AbstractSeriesDAO seriesDAO = DaoFactory.getInstance().getSeriesDAO();
        Criteria criteria = getDefaultObservationCriteria(session);
        Criteria seriesCriteria = criteria.createCriteria(AbstractSeriesObservation.SERIES);
        seriesDAO.addProcedureToCriteria(seriesCriteria, procedure);
        seriesDAO.addObservablePropertyToCriteria(seriesCriteria, observableProperty);
        return criteria;
    }

    @Override
    public Criteria getObservationCriteriaFor(String procedure, String observableProperty, String featureOfInterest,
            Session session) throws CodedException {
        AbstractSeriesDAO seriesDAO = DaoFactory.getInstance().getSeriesDAO();
        Criteria criteria = getDefaultObservationCriteria(session);
        Criteria seriesCriteria = criteria.createCriteria(AbstractSeriesObservation.SERIES);
        seriesDAO.addFeatureOfInterestToCriteria(seriesCriteria, featureOfInterest);
        seriesDAO.addProcedureToCriteria(seriesCriteria, procedure);
        seriesDAO.addObservablePropertyToCriteria(seriesCriteria, observableProperty);
        return criteria;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<String> getObservationIdentifiers(String procedureIdentifier, Session session) {
        Criteria criteria =
                getDefaultObservationInfoCriteria(session)
                        .setProjection(Projections.distinct(Projections.property(ContextualReferencedSeriesObservation.IDENTIFIER)))
                        .add(Restrictions.isNotNull(ContextualReferencedSeriesObservation.IDENTIFIER))
                        .add(Restrictions.eq(ContextualReferencedSeriesObservation.DELETED, false));
        Criteria seriesCriteria = criteria.createCriteria(ContextualReferencedSeriesObservation.SERIES);
        seriesCriteria.createCriteria(Series.PROCEDURE)
                .add(Restrictions.eq(Procedure.IDENTIFIER, procedureIdentifier));
        LOGGER.debug("QUERY getObservationIdentifiers(procedureIdentifier): {}",
                HibernateHelper.getSqlString(criteria));
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Geometry> getSamplingGeometries(String feature, Session session) {
        Criteria criteria = getDefaultObservationTimeCriteria(session).createAlias(AbstractSeriesObservation.SERIES, "s");
        criteria.createCriteria("s." + Series.FEATURE_OF_INTEREST).add(eq(FeatureOfInterest.IDENTIFIER, feature));
        criteria.addOrder(Order.asc(AbstractTemporalReferencedObservation.PHENOMENON_TIME_START));
        criteria.setProjection(Projections.property(AbstractTemporalReferencedObservation.SAMPLING_GEOMETRY));
        return criteria.list();
    }

    /**
     * Create series observation query criteria for series and offerings
     *
     * @param clazz
     *            Class to query
     * @param series
     *            Series to get values for
     * @param offerings
     *            Offerings to get values for
     * @param session
     *            Hibernate session
     * @return Criteria to query series observations
     */
    protected Criteria createCriteriaFor(Class<?> clazz, Series series, List<String> offerings, Session session) {
        final Criteria criteria = createCriteriaFor(clazz, series, session);
        if (CollectionHelper.isNotEmpty(offerings)) {
            criteria.createCriteria(AbstractSeriesObservation.OFFERINGS).add(Restrictions.in(Offering.IDENTIFIER, offerings));
        }
        return criteria;
    }

    /**
     * Create series observation query criteria for series
     *
     * @param clazz
     *            to query
     * @param series
     *            Series to get values for
     * @param session
     *            Hibernate session
     * @return Criteria to query series observations
     */
    protected Criteria createCriteriaFor(Class<?> clazz, Series series, Session session) {
        final Criteria criteria = getDefaultObservationCriteria(session);
        criteria.createCriteria(AbstractSeriesObservation.SERIES).add(Restrictions.eq(Series.ID, series.getSeriesId()));
        return criteria;
    }

    /**
     * Get the result times for this series, offerings and filters
     *
     * @param series
     *            Time series to get result times for
     * @param offerings
     *            Offerings to restrict matching result times
     * @param filter
     *            Temporal filter to restrict matching result times
     * @param session
     *            Hibernate session
     * @return Matching result times
     */
    @SuppressWarnings("unchecked")
    public List<Date> getResultTimesForSeriesObservation(Series series, List<String> offerings, Criterion filter,
            Session session) {
        Criteria criteria = createCriteriaFor(getObservationFactory().temporalReferencedClass(), series, session);
        if (CollectionHelper.isNotEmpty(offerings)) {
            criteria.createCriteria(TemporalReferencedSeriesObservation.OFFERINGS)
                    .add(Restrictions.in(Offering.IDENTIFIER, offerings));
        }
        if (filter != null) {
            criteria.add(filter);
        }
        criteria.setProjection(Projections.distinct(Projections.property(TemporalReferencedSeriesObservation.RESULT_TIME)));
        criteria.addOrder(Order.asc(TemporalReferencedSeriesObservation.RESULT_TIME));
        LOGGER.debug("QUERY getResultTimesForSeriesObservation({}): {}", HibernateHelper.getSqlString(criteria));
        return criteria.list();
    }

    /**
     * Create criteria to query min/max time for series from series observation
     *
     * @param series
     *            Series to get values for
     * @param offerings
     * @param session
     *            Hibernate session
     * @return Criteria to get min/max time values for series
     */
    public Criteria getMinMaxTimeCriteriaForSeriesObservation(Series series, Collection<String> offerings,
            Session session) {
        Criteria criteria = createCriteriaFor(getObservationFactory().temporalReferencedClass(), series, session);
        if (CollectionHelper.isNotEmpty(offerings)) {
            criteria.createCriteria(TemporalReferencedSeriesObservation.OFFERINGS).add(
                    Restrictions.in(Offering.IDENTIFIER, offerings));
        }
        criteria.setProjection(Projections.projectionList()
                .add(Projections.min(TemporalReferencedSeriesObservation.PHENOMENON_TIME_START))
                .add(Projections.max(TemporalReferencedSeriesObservation.PHENOMENON_TIME_END)));
        return criteria;
    }

    public ScrollableResults getSeriesNotMatchingSeries(Set<Long> seriesIDs, GetObservationRequest request,
            Set<String> features, Criterion temporalFilterCriterion, Session session) throws OwsExceptionReport {
        Criteria c = getSeriesObservationCriteriaFor(request, features, temporalFilterCriterion, null, session);
        c.createCriteria(AbstractSeriesObservation.SERIES)
            .add(Restrictions.not(Restrictions.in(Series.ID, seriesIDs)));
        c.setProjection(Projections.property(AbstractSeriesObservation.SERIES));
        return c.setReadOnly(true).scroll(ScrollMode.FORWARD_ONLY);
    }

    public ScrollableResults getSeriesNotMatchingSeries(Set<Long> seriesIDs, GetObservationRequest request,
            Set<String> features, Session session) throws OwsExceptionReport {
        return getSeriesNotMatchingSeries(seriesIDs, request, features, null, session);
    }

    /**
     * Create series observations {@link Criteria} for GetObservation request, features, and filter criterion (typically a temporal filter) or
     * an indeterminate time (first/latest). This method is private and accepts all possible arguments for request-based
     * getSeriesObservationFor. Other public methods overload this method with sensible combinations of arguments.
     *
     * @param request
     *              GetObservation request
     * @param features
     *              Collection of feature identifiers resolved from the request
     * @param filterCriterion
     *              Criterion to apply to criteria query (typically a temporal filter)
     * @param sosIndeterminateTime
     *              Indeterminate time to use in a temporal filter (first/latest)
     * @param session
     * @return Series observations {@link Criteria}
     * @throws OwsExceptionReport
     */
    protected Criteria getSeriesObservationCriteriaFor(GetObservationRequest request, Collection<String> features,
                Criterion filterCriterion, SosIndeterminateTime sosIndeterminateTime, Session session) throws OwsExceptionReport {
            final Criteria observationCriteria = getDefaultObservationCriteria(session);


            Criteria seriesCriteria = observationCriteria.createCriteria(AbstractSeriesObservation.SERIES);

            checkAndAddSpatialFilteringProfileCriterion(observationCriteria, request, session);

            if (CollectionHelper.isNotEmpty(request.getProcedures())) {
                seriesCriteria.createCriteria(Series.PROCEDURE)
                        .add(Restrictions.in(Procedure.IDENTIFIER, request.getProcedures()));
            }

            if (CollectionHelper.isNotEmpty(request.getObservedProperties())) {
                seriesCriteria.createCriteria(Series.OBSERVABLE_PROPERTY)
                        .add(Restrictions.in(ObservableProperty.IDENTIFIER, request.getObservedProperties()));
            }

            if (CollectionHelper.isNotEmpty(features)) {
                seriesCriteria.createCriteria(Series.FEATURE_OF_INTEREST)
                        .add(Restrictions.in(FeatureOfInterest.IDENTIFIER, features));
            }

            if (CollectionHelper.isNotEmpty(request.getOfferings())) {
                observationCriteria.createCriteria(AbstractSeriesObservation.OFFERINGS)
                        .add(Restrictions.in(Offering.IDENTIFIER, request.getOfferings()));
            }

            String logArgs = "request, features, offerings";
            if (filterCriterion != null) {
                logArgs += ", filterCriterion";
                observationCriteria.add(filterCriterion);
            }
            if (sosIndeterminateTime != null) {
                logArgs += ", sosIndeterminateTime";
                addIndeterminateTimeRestriction(observationCriteria, sosIndeterminateTime);
            }
            LOGGER.debug("QUERY getSeriesObservationFor({}): {}", logArgs, HibernateHelper.getSqlString(observationCriteria));
            return observationCriteria;
    }

    /**
     * Query series observations {@link ScrollableResults} for GetObservation request and features
     *
     * @param request
     *              GetObservation request
     * @param features
     *              Collection of feature identifiers resolved from the request
     * @param session
     *              Hibernate session
     * @return {@link ScrollableResults} of Series observations that fit
     * @throws OwsExceptionReport
     */
    public ScrollableResults getStreamingSeriesObservationsFor(GetObservationRequest request, Collection<String> features,
            Session session) throws OwsExceptionReport {
        return getStreamingSeriesObservationsFor(request, features, null, null, session);
    }

    /**
     * Query series observations {@link ScrollableResults} for GetObservation request, features, and a filter criterion (typically a temporal filter)
     *
     * @param request
     *              GetObservation request
     * @param features
     *              Collection of feature identifiers resolved from the request
     * @param filterCriterion
     *              Criterion to apply to criteria query (typically a temporal filter)
     * @param session
     *              Hibernate session
     * @return {@link ScrollableResults} of Series observations that fit
     * @throws OwsExceptionReport
     */
    public ScrollableResults getStreamingSeriesObservationsFor(GetObservationRequest request, Collection<String> features,
            Criterion filterCriterion, Session session) throws OwsExceptionReport {
        return getStreamingSeriesObservationsFor(request, features, filterCriterion, null, session);
    }

    /**
     * Query series observations for GetObservation request, features, and filter criterion (typically a temporal filter) or
     * an indeterminate time (first/latest). This method is private and accepts all possible arguments for request-based
     * getSeriesObservationFor. Other public methods overload this method with sensible combinations of arguments.
     *
     * @param request
     *              GetObservation request
     * @param features
     *              Collection of feature identifiers resolved from the request
     * @param filterCriterion
     *              Criterion to apply to criteria query (typically a temporal filter)
     * @param sosIndeterminateTime
     *              Indeterminate time to use in a temporal filter (first/latest)
     * @param session
     * @return {@link ScrollableResults} of Series observations that fits
     * @throws OwsExceptionReport
     */
    protected ScrollableResults getStreamingSeriesObservationsFor(GetObservationRequest request, Collection<String> features,
            Criterion filterCriterion, SosIndeterminateTime sosIndeterminateTime, Session session) throws OwsExceptionReport {
        return getSeriesObservationCriteriaFor(request, features, filterCriterion, sosIndeterminateTime, session).setReadOnly(true).scroll(ScrollMode.FORWARD_ONLY);
    }

    /**
     * Update series observation by setting deleted flag
     *
     * @param series
     *            Series for which the observations should be updated
     * @param deleteFlag
     *            New deleted flag value
     * @param session
     *            Hibernate Session
     */
    public void updateObservationSetAsDeletedForSeries(List<Series> series, boolean deleteFlag, Session session) {
        if (CollectionHelper.isNotEmpty(series)) {
            Criteria criteria = getDefaultObservationCriteria(session);
            criteria.add(Restrictions.in(SeriesObservation.SERIES, series));
            ScrollableIterable<SeriesObservation<?>> scroll = ScrollableIterable.fromCriteria(criteria);
            updateObservation(scroll, deleteFlag, session);
        }
    }

    /**
     * Query the min time from series observations for series
     *
     * @param series
     *            Series to get values for
     * @param session
     *            Hibernate session
     * @return Min time from series observations
     */
    public DateTime getMinSeriesObservationTime(Series series, Session session) {
        Criteria criteria = createCriteriaFor(getObservationFactory().temporalReferencedClass(), series, session);
        criteria.setProjection(Projections.min(TemporalReferencedSeriesObservation.PHENOMENON_TIME_START));
        Object min = criteria.uniqueResult();
        if (min != null) {
            return new DateTime(min, DateTimeZone.UTC);
        }
        return null;
    }

    /**
     * Query the max time from series observations for series
     *
     * @param series
     *            Series to get values for
     * @param session
     *            Hibernate session
     * @return Max time from series observations
     */
    public DateTime getMaxSeriesObservationTime(Series series, Session session) {
        Criteria criteria = createCriteriaFor(getObservationFactory().temporalReferencedClass(), series, session);
        criteria.setProjection(Projections.max(TemporalReferencedSeriesObservation.PHENOMENON_TIME_END));
        Object max = criteria.uniqueResult();
        if (max != null) {
            return new DateTime(max, DateTimeZone.UTC);
        }
        return null;
    }

    /**
     * Query series observation for series and offerings
     *
     * @param series
     *            Series to get values for
     * @param offerings
     *            Offerings to get values for
     * @param session
     *            Hibernate session
     * @return Series observations that fit
     */
    public abstract List<SeriesObservation<?>> getSeriesObservationFor(Series series, List<String> offerings, Session session);

    /**
     * Query series obserations for series, temporal filter, and offerings
     *
     * @param series
     *            Series to get values for
     * @param offerings
     *            Offerings to get values for
     * @param filterCriterion
     * @param session
     *            Hibernate session
     * @return Series observations that fit
     */
    public abstract List<SeriesObservation<?>> getSeriesObservationFor(Series series, List<String> offerings, Criterion filterCriterion, Session session);

    /**
     * Query first/latest series obserations for series (and offerings)
     *
     * @param series
     *            Series to get values for
     * @param offerings
     *            Offerings to get values for
     * @param sosIndeterminateTime
     * @param session
     *            Hibernate session
     * @return Series observations that fit
     */
    public abstract List<SeriesObservation<?>> getSeriesObservationForSosIndeterminateTimeFilter(Series series, List<String> offerings, SosIndeterminateTime sosIndeterminateTime, Session session);

    /**
     * Query series observations for GetObservation request and features
     *
     * @param request
     *            GetObservation request
     * @param features
     *            Collection of feature identifiers resolved from the request
     * @param session
     *            Hibernate session
     * @return Series observations that fit
     * @throws OwsExceptionReport
     */
    public abstract List<SeriesObservation<?>> getSeriesObservationsFor(GetObservationRequest request, Collection<String> features, Session session) throws OwsExceptionReport;

    /**
     * Query series observations for GetObservation request, features, and a
     * filter criterion (typically a temporal filter)
     *
     * @param request
     *            GetObservation request
     * @param features
     *            Collection of feature identifiers resolved from the request
     * @param filterCriterion
     *            Criterion to apply to criteria query (typically a temporal
     *            filter)
     * @param session
     *            Hibernate session
     * @return Series observations that fit
     * @throws OwsExceptionReport
     */
    public abstract List<SeriesObservation<?>> getSeriesObservationsFor(GetObservationRequest request, Collection<String> features, Criterion filterCriterion, Session session) throws OwsExceptionReport;

    /**
     * Query series observations for GetObservation request, features, and an
     * indeterminate time (first/latest)
     *
     * @param request
     *            GetObservation request
     * @param features
     *            Collection of feature identifiers resolved from the request
     * @param sosIndeterminateTime
     *            Indeterminate time to use in a temporal filter (first/latest)
     * @param session
     *            Hibernate session
     * @return Series observations that fit
     * @throws OwsExceptionReport
     */
    public abstract List<SeriesObservation<?>> getSeriesObservationsFor(GetObservationRequest request, Collection<String> features, SosIndeterminateTime sosIndeterminateTime, Session session) throws OwsExceptionReport;

    /**
     * Query series observations for GetObservation request, features, and
     * filter criterion (typically a temporal filter) or an indeterminate time
     * (first/latest). This method is private and accepts all possible arguments
     * for request-based getSeriesObservationFor. Other public methods overload
     * this method with sensible combinations of arguments.
     *
     * @param request
     *            GetObservation request
     * @param features
     *            Collection of feature identifiers resolved from the request
     * @param filterCriterion
     *            Criterion to apply to criteria query (typically a temporal
     *            filter)
     * @param sosIndeterminateTime
     *            Indeterminate time to use in a temporal filter (first/latest)
     * @param session
     * @return Series observations that fit
     * @throws OwsExceptionReport
     */
    protected abstract List<? extends SeriesObservation<?>> getSeriesObservationsFor(GetObservationRequest request, Collection<String> features, Criterion filterCriterion, SosIndeterminateTime sosIndeterminateTime, Session session) throws OwsExceptionReport;


    public abstract List<SeriesObservation<?>> getSeriesObservationsFor(Series series, GetObservationRequest request, SosIndeterminateTime sosIndeterminateTime, Session session) throws OwsExceptionReport;

    protected Criteria getSeriesObservationCriteriaFor(Series series, GetObservationRequest request,
            SosIndeterminateTime sosIndeterminateTime, Session session) throws OwsExceptionReport {
        final Criteria c =
                getDefaultObservationCriteria(session).add(
                        Restrictions.eq(AbstractSeriesObservation.SERIES, series));
        checkAndAddSpatialFilteringProfileCriterion(c, request, session);

        if (request.isSetOffering()) {
            c.createCriteria(AbstractSeriesObservation.OFFERINGS).add(
                    Restrictions.in(Offering.IDENTIFIER, request.getOfferings()));
        }
        String logArgs = "request, features, offerings";
        logArgs += ", sosIndeterminateTime";
        addIndeterminateTimeRestriction(c, sosIndeterminateTime);
        LOGGER.debug("QUERY getSeriesObservationFor({}): {}", logArgs, HibernateHelper.getSqlString(c));
        return c;

    }

    protected Criteria getSeriesObservationCriteriaForSosIndeterminateTimeFilter(Series series,
            List<String> offerings, SosIndeterminateTime sosIndeterminateTime, Session session) {
        final Criteria criteria = createCriteriaFor(getObservationFactory().observationClass(), series, offerings, session);
        criteria.addOrder(getOrder(sosIndeterminateTime)).setMaxResults(1);
        LOGGER.debug("QUERY getSeriesObservationForSosIndeterminateTimeFilter(series, offerings,(first,latest)): {}",
                HibernateHelper.getSqlString(criteria));
        return criteria;
    }

    protected Criteria getSeriesObservationCriteriaFor(Series series, List<String> offerings,
            Criterion filterCriterion, Session session) {
        final Criteria criteria = createCriteriaFor(getObservationFactory().observationClass(), series, offerings, session);
        criteria.add(filterCriterion);
        LOGGER.debug("QUERY getSeriesObservationFor(series, offerings, temporalFilter): {}",
                HibernateHelper.getSqlString(criteria));
        return criteria;
    }

    protected Criteria getSeriesObservationCriteriaFor(Series series, List<String> offerings,
            Session session) {
        final Criteria criteria = createCriteriaFor(AbstractSeriesObservation.class, series, offerings, session);
        LOGGER.debug("QUERY getSeriesObservationFor(series, offerings): {}", HibernateHelper.getSqlString(criteria));
        return criteria;
    }
}
