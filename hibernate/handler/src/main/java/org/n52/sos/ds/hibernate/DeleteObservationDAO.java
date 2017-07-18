package org.n52.sos.ds.hibernate;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import org.hibernate.HibernateException;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.n52.iceland.convert.ConverterException;
import org.n52.iceland.ds.ConnectionProvider;
import org.n52.iceland.i18n.I18NDAORepository;
import org.n52.iceland.ogc.ows.OwsServiceMetadataRepository;
import org.n52.iceland.util.LocalizedProducer;
import org.n52.shetland.ogc.om.OmObservation;
import org.n52.shetland.ogc.ows.OwsServiceProvider;
import org.n52.shetland.ogc.ows.exception.InvalidParameterValueException;
import org.n52.shetland.ogc.ows.exception.NoApplicableCodeException;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.sos.delobs.DeleteObservationConstants;
import org.n52.shetland.ogc.sos.delobs.DeleteObservationRequest;
import org.n52.shetland.ogc.sos.delobs.DeleteObservationResponse;
import org.n52.shetland.ogc.sos.request.AbstractObservationRequest;
import org.n52.shetland.ogc.sos.request.GetObservationRequest;
import org.n52.shetland.util.CollectionHelper;
import org.n52.sos.ds.AbstractDeleteObservationHandler;
import org.n52.sos.ds.hibernate.dao.DaoFactory;
import org.n52.sos.ds.hibernate.dao.observation.series.SeriesDAO;
import org.n52.sos.ds.hibernate.entities.observation.Observation;
import org.n52.sos.ds.hibernate.entities.observation.full.ComplexObservation;
import org.n52.sos.ds.hibernate.entities.observation.full.ProfileObservation;
import org.n52.sos.ds.hibernate.entities.observation.series.Series;
import org.n52.sos.ds.hibernate.entities.observation.series.SeriesObservation;
import org.n52.sos.ds.hibernate.util.SosTemporalRestrictions;
import org.n52.sos.ds.hibernate.util.TemporalRestrictions;
import org.n52.sos.ds.hibernate.util.observation.HibernateObservationUtilities;

import com.google.common.base.Joiner;

public class DeleteObservationDAO
        extends AbstractDeleteObservationHandler {

    private HibernateSessionHolder sessionHolder;

    private OwsServiceMetadataRepository serviceMetadataRepository;

    private DaoFactory daoFactory;

    private I18NDAORepository i18NDAORepository;

    @Inject
    public void setDaoFactory(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    @Inject
    public void setServiceMetadataRepository(OwsServiceMetadataRepository repo) {
        this.serviceMetadataRepository = repo;
    }

    @Inject
    public void setConnectionProvider(ConnectionProvider connectionProvider) {
        this.sessionHolder = new HibernateSessionHolder(connectionProvider);
    }

    @Inject
    public void setI18NDAORepository(I18NDAORepository i18NDAORepository) {
        this.i18NDAORepository = i18NDAORepository;
    }

    @Override
    public synchronized DeleteObservationResponse deleteObservation(DeleteObservationRequest request)
            throws OwsExceptionReport {
        DeleteObservationResponse response = new DeleteObservationResponse(request.getResponseFormat());
        response.setService(request.getService());
        response.setVersion(request.getVersion());
        Session session = null;
        Transaction transaction = null;
        try {
            session = sessionHolder.getSession();
            transaction = session.beginTransaction();
            if (request.isSetObservationIdentifiers()) {
                deleteObservationsByIdentifier(request, response, session);
            } else {
                deleteObservationByParameter(request, response, session);
            }
            transaction.commit();
        } catch (HibernateException he) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new NoApplicableCodeException().causedBy(he)
                    .withMessage("Error while updating deleted observation flag data!");
        } catch (ConverterException ce) {
            throw new NoApplicableCodeException().causedBy(ce)
                    .withMessage("Error while updating deleted observation flag data!");
        } finally {
            sessionHolder.returnSession(session);
        }
        return response;
    }

    private AbstractObservationRequest getRequest(DeleteObservationRequest request) {
        return (AbstractObservationRequest) new GetObservationRequest().setService(request.getService())
                .setVersion(request.getVersion());
    }

    private void deleteObservationsByIdentifier(DeleteObservationRequest request, DeleteObservationResponse response,
            Session session) throws OwsExceptionReport, ConverterException {
        Set<String> ids = request.getObservationIdentifiers();
        List<Observation<?>> observations = daoFactory.getObservationDAO().getObservationByIdentifiers(ids, session);
        if (CollectionHelper.isNotEmpty(observations)) {
            for (Observation<?> observation : observations) {
                delete(observation, session);
            }
            if (DeleteObservationConstants.NS_SOSDO_1_0.equals(request.getResponseFormat())) {
                Observation<?> observation = observations.iterator().next();
                Set<Observation<?>> oberservations = Collections.singleton(observation);
                LocalizedProducer<OwsServiceProvider> serviceProvider =
                        this.serviceMetadataRepository.getServiceProviderFactory(request.getService());
                Locale locale = getRequestedLocale(request);
                OmObservation so = HibernateObservationUtilities.createSosObservationsFromObservations(oberservations,
                        getRequest(request), serviceProvider, i18NDAORepository, null, daoFactory, session).next();
                response.setObservationId(request.getObservationIdentifiers().iterator().next());
                response.setDeletedObservation(so);
            }
        } else {
            if (DeleteObservationConstants.NS_SOSDO_1_0.equals(request.getResponseFormat())) {
                throw new InvalidParameterValueException(DeleteObservationConstants.PARAM_OBSERVATION,
                        Joiner.on(", ").join(request.getObservationIdentifiers()));
            }
        }
    }

    private void deleteObservationByParameter(DeleteObservationRequest request, DeleteObservationResponse response,
            Session session) throws OwsExceptionReport {
        Criterion filter = null;
        if (CollectionHelper.isNotEmpty(request.getTemporalFilters())) {
            filter = SosTemporalRestrictions.filter(request.getTemporalFilters());
        }
        ScrollableResults result = daoFactory.getObservationDAO().getObservations(request.getProcedures(),
                request.getObservedProperties(), request.getFeatureIdentifiers(), request.getOfferings(), filter,
                session);
        while (result.next()) {
            delete((Observation<?>) result.get()[0], session);
        }
    }

    private void delete(Observation<?> observation, Session session) {
        if (observation != null) {
            if (observation instanceof ComplexObservation) {
                for (Observation<?> o : ((ComplexObservation) observation).getValue()) {
                    delete(o, session);
                }
            } else if (observation instanceof ProfileObservation) {
                for (Observation<?> o : ((ProfileObservation) observation).getValue()) {
                    delete(o, session);
                }
            }
            observation.setDeleted(true);
            session.saveOrUpdate(observation);
            checkSeriesForFirstLatest(observation, session);
            session.flush();
        }
    }

    /**
     * Check if {@link Series} should be updated
     *
     * @param observation
     *            Deleted observation
     * @param session
     *            Hibernate session
     */
    private void checkSeriesForFirstLatest(Observation<?> observation, Session session) {
        if (observation instanceof SeriesObservation) {
            Series series = ((SeriesObservation) observation).getSeries();
            if ((series.isSetFirstTimeStamp()
                    && series.getFirstTimeStamp().equals(observation.getPhenomenonTimeStart()))
                    || (series.isSetLastTimeStamp()
                            && series.getLastTimeStamp().equals(observation.getPhenomenonTimeEnd()))) {
                new SeriesDAO(daoFactory).updateSeriesAfterObservationDeletion(series, (SeriesObservation) observation,
                        session);
            }
        }
    }

}
