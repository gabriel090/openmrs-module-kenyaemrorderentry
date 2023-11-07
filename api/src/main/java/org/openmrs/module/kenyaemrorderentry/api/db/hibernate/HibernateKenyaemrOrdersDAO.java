package org.openmrs.module.kenyaemrorderentry.api.db.hibernate;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.math.BigInteger;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.DataException;
import org.openmrs.Cohort;
import org.openmrs.Order;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.kenyaemrorderentry.api.db.KenyaemrOrdersDAO;
import org.openmrs.module.kenyaemrorderentry.manifest.LabManifest;
import org.openmrs.module.kenyaemrorderentry.manifest.LabManifestOrder;
import org.openmrs.module.reporting.common.DurationUnit;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.transaction.annotation.Transactional;

import org.openmrs.module.kenyaemrorderentry.util.Utils;

public class HibernateKenyaemrOrdersDAO implements KenyaemrOrdersDAO {
    protected final Log log = LogFactory.getLog(this.getClass());

    private SessionFactory sessionFactory;
    /**
     * @Autowired private LabOrderManifestDao labOrderManifestDao;
     */
    /**
     * @param sessionFactory the sessionFactory to set
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @Override
    public LabManifest saveLabOrderManifest(LabManifest labManifest) throws DAOException {
        System.err.println("Saving the labManifest");
        sessionFactory.getCurrentSession().saveOrUpdate(labManifest);
        return labManifest;
    }

    @Override
    public Long getLastManifestID() {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.setProjection(Projections.max("id"));
        Long maxId = ((Number)criteria.uniqueResult()).longValue();
        return(maxId);
    }

    @Override
    public List<LabManifest> getLabOrderManifest() throws DataException {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.add(Restrictions.eq("voided", false));
        criteria.addOrder(org.hibernate.criterion.Order.desc("id"));
        return criteria.list();
    }

    @Override
    public LabManifest getLabOrderManifestById(Integer id) throws DataException {
        return (LabManifest) this.sessionFactory.getCurrentSession().get(LabManifest.class, id);
    }

    @Override
    public LabManifest getLabManifestById(Integer manID) throws DataException {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.add(Restrictions.eq("id", manID));
        return (LabManifest) criteria.uniqueResult();
    }

    @Override
    public String getLabManifestStatusByIdSQL(Integer manID) throws DataException {
        Context.addProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
        String manifest = null;
        String sql = "select status from kenyaemr_order_entry_lab_manifest where id = " + manID;

        List<List<Object>> manifests = Context.getAdministrationService().executeSQL(sql, true);
        if ( manifests != null && !manifests.isEmpty()) {
            for (List<Object> res : manifests) {
                manifest = (String) res.get(0);
                break;
            }
        }
        Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
        return manifest;
    }

    @Override
    public List<SimpleObject> getLabManifestSummaryGraphSQL() throws DataException {
        Context.addProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
        List<SimpleObject> ret = new ArrayList<SimpleObject>();
        String sql = "SELECT CONVERT(YEAR(date_sent), CHAR) AS year, " +
                "CONVERT(AVG(CASE WHEN MONTH(date_sent) = 1 THEN DATEDIFF(result_date, date_sent) END), CHAR) AS Jan, " +
                "CONVERT(AVG(CASE WHEN MONTH(date_sent) = 2 THEN DATEDIFF(result_date, date_sent) END), CHAR) AS Feb, " +
                "CONVERT(AVG(CASE WHEN MONTH(date_sent) = 3 THEN DATEDIFF(result_date, date_sent) END), CHAR) AS Mar, " +
                "CONVERT(AVG(CASE WHEN MONTH(date_sent) = 4 THEN DATEDIFF(result_date, date_sent) END), CHAR) AS Apr, " +
                "CONVERT(AVG(CASE WHEN MONTH(date_sent) = 5 THEN DATEDIFF(result_date, date_sent) END), CHAR) AS May, " +
                "CONVERT(AVG(CASE WHEN MONTH(date_sent) = 6 THEN DATEDIFF(result_date, date_sent) END), CHAR) AS Jun, " +
                "CONVERT(AVG(CASE WHEN MONTH(date_sent) = 7 THEN DATEDIFF(result_date, date_sent) END), CHAR) AS Jul, " +
                "CONVERT(AVG(CASE WHEN MONTH(date_sent) = 8 THEN DATEDIFF(result_date, date_sent) END), CHAR) AS Aug, " +
                "CONVERT(AVG(CASE WHEN MONTH(date_sent) = 9 THEN DATEDIFF(result_date, date_sent) END), CHAR) AS Sep, " +
                "CONVERT(AVG(CASE WHEN MONTH(date_sent) = 10 THEN DATEDIFF(result_date, date_sent) END), CHAR) AS Oct, " +
                "CONVERT(AVG(CASE WHEN MONTH(date_sent) = 11 THEN DATEDIFF(result_date, date_sent) END), CHAR) AS Nov, " +
                "CONVERT(AVG(CASE WHEN MONTH(date_sent) = 12 THEN DATEDIFF(result_date, date_sent) END), CHAR) AS `Dec` " +
                "FROM kenyaemr_order_entry_lab_manifest_order " +
                "WHERE status = 'Complete' " +
                "AND date_sent >= DATE_SUB(CURDATE(), INTERVAL 4 YEAR) " +
                "GROUP BY YEAR(date_sent)";
        try {
            List<List<Object>> manifests = Context.getAdministrationService().executeSQL(sql, true);
            if ( manifests != null && !manifests.isEmpty() ) {
                for (List<Object> res : manifests) {
                    try {
                        String year = res.get(0) != null ? (String) res.get(0) : "0";
                        String jan = res.get(1) != null ? (String) res.get(1) : "0";
                        String feb = res.get(2) != null ? (String) res.get(2) : "0";
                        String mar = res.get(3) != null ? (String) res.get(3) : "0";
                        String apr = res.get(4) != null ? (String) res.get(4) : "0";
                        String may = res.get(5) != null ? (String) res.get(5) : "0";
                        String jun = res.get(6) != null ? (String) res.get(6) : "0";
                        String jul = res.get(7) != null ? (String) res.get(7) : "0";
                        String aug = res.get(8) != null ? (String) res.get(8) : "0";
                        String sep = res.get(9) != null ? (String) res.get(9) : "0";
                        String oct = res.get(10) != null ? (String) res.get(10) : "0";
                        String nov = res.get(11) != null ? (String) res.get(11) : "0";
                        String dec = res.get(12) != null ? (String) res.get(12) : "0";
                        SimpleObject tmp = new SimpleObject();
                        tmp.put("year", Utils.getIntegerValue(year));
                        tmp.put("jan", Utils.getIntegerValue(jan));
                        tmp.put("feb", Utils.getIntegerValue(feb));
                        tmp.put("mar", Utils.getIntegerValue(mar));
                        tmp.put("apr", Utils.getIntegerValue(apr));
                        tmp.put("may", Utils.getIntegerValue(may));
                        tmp.put("jun", Utils.getIntegerValue(jun));
                        tmp.put("jul", Utils.getIntegerValue(jul));
                        tmp.put("aug", Utils.getIntegerValue(aug));
                        tmp.put("sep", Utils.getIntegerValue(sep));
                        tmp.put("oct", Utils.getIntegerValue(oct));
                        tmp.put("nov", Utils.getIntegerValue(nov));
                        tmp.put("dec", Utils.getIntegerValue(dec));
                        ret.add(tmp);
                    } catch(Exception ex) {
                        System.err.println("Graph entry Error : " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
        } catch(Exception ex) {
            System.err.println("Error getting graph data: " + ex.getMessage());
        }
        Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
        
        return ret;
    }

    @Override
    public LabManifest getLabOrderManifestByManifestType(Integer manifestType) throws DataException {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("manifestType", manifestType));
        criteria.addOrder(org.hibernate.criterion.Order.asc("id"));
        return (LabManifest) criteria.uniqueResult();
    }

    @Override
    public LabManifest getLastLabOrderManifest() throws DataException {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.setMaxResults(1);
        criteria.addOrder(org.hibernate.criterion.Order.desc("id"));
        return (LabManifest) criteria.uniqueResult();
    }

    @Override
    public List<LabManifest> getLabOrderManifestBetweenDates(Date startDate, Date endDate) throws DataException{
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.add(Restrictions.ge("startDate",startDate));
        criteria.add(Restrictions.le("endDate",endDate));
        criteria.add(Restrictions.eq("voided", false));

        return criteria.list();
    }

    @Override
    public void voidLabOrderManifest(Integer id) throws DAOException {
        LabManifest mf =  (LabManifest) this.sessionFactory.getCurrentSession().get(LabManifest.class, id);
        mf.setVoided(true);
        sessionFactory.getCurrentSession().saveOrUpdate(mf);
    }

    //Lab minifest orders methods
    @Override
    public LabManifestOrder saveLabManifestOrder(LabManifestOrder labManifestOrder) throws DAOException {
        sessionFactory.getCurrentSession().saveOrUpdate(labManifestOrder);
        return labManifestOrder;
    }

    @Override
    public List<LabManifestOrder> getLabManifestOrders() throws DataException {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("voided", false));
        return criteria.list();
    }

    @Override
    public List<LabManifestOrder> getLabManifestOrderByManifest(LabManifest labManifest) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("labManifest", labManifest));
        criteria.add(Restrictions.eq("voided", false));
        //criteria.addOrder(org.hibernate.criterion.Order.asc("id"));
        criteria.addOrder(org.hibernate.criterion.Order.asc("dateSent"));
        return criteria.list();
    }

    @Override
    public Integer countTotalSamples(LabManifest labManifest) {
        Integer ret = 0;
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("labManifest", labManifest));
        criteria.add(Restrictions.eq("voided", false));
        ret = criteria.list().size();
        return(ret);
    }

    @Override
    public Integer countSamplesSuppressed(LabManifest labManifest) {
        Integer ret = 0;
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("labManifest", labManifest));
        criteria.add(Restrictions.eq("voided", false));
        List<LabManifestOrder> allOrders = criteria.list();
        for (LabManifestOrder labManifestOrder : allOrders) {
            String results = labManifestOrder.getResult();
            if(results != null) {
                results = results.toLowerCase().trim();
                if(results.contains("ldl") || results.equalsIgnoreCase("ldl")) {
                    ret++;
                } else if(results.endsWith("copies/ml")) {
                    int index = results.indexOf("copies/ml");
                    if (index != -1) {
                        String val = results.substring(0, index); // Get the 40 from (40 copies/ml)
                        val = val.trim();
                        Integer vlVal = Utils.getIntegerValue(val);
                        if(vlVal >= 200) {
                            // unsuppressed
                        } else {
                            ret++;
                        }
                    }
                } else {
                    Integer vlVal = Utils.getIntegerValue(results);
                    if(vlVal >= 200) {
                        // unsuppressed
                    } else {
                        ret++;
                    }
                }
            }
        }
        return(ret);
    }

    @Override
    public Integer countSamplesUnsuppressed(LabManifest labManifest) {
        Integer ret = 0;
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("labManifest", labManifest));
        criteria.add(Restrictions.eq("voided", false));
        List<LabManifestOrder> allOrders = criteria.list();
        for (LabManifestOrder labManifestOrder : allOrders) {
            String results = labManifestOrder.getResult();
            if(results != null) {
                results = results.toLowerCase().trim();
                if(results.contains("ldl") || results.equalsIgnoreCase("ldl")) {
                    // suppressed
                } else if(results.endsWith("copies/ml")) {
                    int index = results.indexOf("copies/ml");
                    if (index != -1) {
                        String val = results.substring(0, index); // Get the 40 from (40 copies/ml)
                        val = val.trim();
                        Integer vlVal = Utils.getIntegerValue(val);
                        if(vlVal >= 200) {
                            ret++;
                        } else {
                            // suppressed
                        }
                    }
                } else {
                    Integer vlVal = Utils.getIntegerValue(results);
                    if(vlVal >= 200) {
                        ret++;
                    } else {
                        // suppressed
                    }
                }
            }
        }
        return(ret);
    }

    @Override
    public Integer countSamplesRejected(LabManifest labManifest) {
        Integer ret = 0;
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("labManifest", labManifest));
        criteria.add(Restrictions.eq("voided", false));
        criteria.add(Restrictions.eq("status", "Rejected"));
        ret = criteria.list().size();
        return(ret);
    }

    @Override
    public LabManifestOrder getLabManifestOrderById(Integer id) throws DataException {
        return (LabManifestOrder) this.sessionFactory.getCurrentSession().get(LabManifestOrder.class, id);
    }

    @Override
    public LabManifestOrder getLabManifestOrderByOrderType(Integer orderType) throws DataException {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("orderType", orderType));
        criteria.addOrder(org.hibernate.criterion.Order.asc("id"));
        return (LabManifestOrder) criteria.uniqueResult();
    }

    @Override
    public void voidLabManifestOrder(Integer labManifestOrder) throws DAOException{
        LabManifestOrder mfo =  (LabManifestOrder) this.sessionFactory.getCurrentSession().get(LabManifestOrder.class, labManifestOrder);
        if (mfo != null) {
            sessionFactory.getCurrentSession().delete(mfo);
        }
    }

    @Override
    public LabManifestOrder getLabManifestOrderByManifest(Order specimenId) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("order", specimenId));
        if (criteria.list().size() > 0) {
            return (LabManifestOrder) criteria.list().get(0);
        }
        return null;
    }

    @Override
    public List<LabManifestOrder> getLabManifestOrderByStatus(String status) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("status", status));
        criteria.add(Restrictions.eq("voided", false));
        criteria.addOrder(org.hibernate.criterion.Order.asc("id"));
        return criteria.list();
    }

    @Override
    public List<LabManifestOrder> getLabManifestOrderByManifestAndStatus(LabManifest labManifestOrder, String status) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("labManifest", labManifestOrder));
        criteria.add(Restrictions.eq("status", status));
        criteria.add(Restrictions.eq("voided", false));
        criteria.addOrder(org.hibernate.criterion.Order.asc("id"));
        return criteria.list();
    }

    @Override
    public List<LabManifestOrder> getLabManifestOrderByManifestAndStatus(LabManifest labManifestOrder, String... status) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("labManifest", labManifestOrder));
        criteria.add(Restrictions.in("status", status));
        criteria.add(Restrictions.eq("voided", false));
        criteria.addOrder(org.hibernate.criterion.Order.asc("id"));
        return criteria.list();
    }

    @Override
    public LabManifest getLabOrderManifestByStatus(String status) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.add(Restrictions.eq("status", status));
        criteria.addOrder(org.hibernate.criterion.Order.asc("id"));
        criteria.add(Restrictions.eq("voided", false));
        if (criteria.list().size() > 0) {
            return (LabManifest) criteria.list().get(0);
        }
        return null;
    }

    public Cohort getPatientsWithCadre(boolean includeTroupes, boolean includeCivilians) {

        if (!includeTroupes && !includeCivilians) {
            return new Cohort();
        }

        String prefixTerm = "";
        StringBuilder query = new StringBuilder("select p.personId from Person p,PersonAttribute pa where p.personId = pa.person.personId and p.voided = false and ( ");
        if (includeTroupes) {
            query.append(" pa.value  = 'Troupe' ");
            prefixTerm = " or";
        }
        if (includeCivilians) {
            query.append(prefixTerm + " pa.value = 'Civilian'");
        }

        query.append(")");
        Query q = sessionFactory.getCurrentSession().createQuery(query.toString());
        q.setCacheMode(CacheMode.IGNORE);
        return new Cohort(q.list());
    }

    @Override
    public List<LabManifestOrder> getLabManifestOrdersToSend(LabManifest labManifest) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("labManifest", labManifest));
        criteria.add(Restrictions.eq("status", "Pending"));
        criteria.addOrder(org.hibernate.criterion.Order.asc("id"));
        criteria.add(Restrictions.eq("voided", false));
        criteria.setMaxResults(50);
        return criteria.list();
    }

    @Override
    public Long countLabManifestOrdersToSend(LabManifest labManifest) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("labManifest", labManifest));
        criteria.add(Restrictions.eq("status", "Pending"));
        criteria.add(Restrictions.eq("voided", false));
        criteria.setProjection(Projections.rowCount());
        List rowCount = criteria.list();
        Long count = (Long) rowCount.get(0);
        return(count);
    }

    //Patient contact dimensions methods
    @Override
    public Cohort getPatientContactWithGender(boolean includeMales, boolean includeFemales, boolean includeUnknownGender) {

        if (!includeMales && !includeFemales && !includeUnknownGender) {
            return new Cohort();
        }

        String prefixTerm = "";
        StringBuilder query = new StringBuilder("select c.id from kenyaemr_hiv_testing_patient_contact c where c.voided = 0 and ( ");
        if (includeMales) {
            query.append(" c.sex = 'M' ");
            prefixTerm = " or";
        }
        if (includeFemales) {
            query.append(prefixTerm + " c.sex = 'F'");
            prefixTerm = " or";
        }
        if (includeUnknownGender) {
            query.append(prefixTerm + " c.sex is null or (c.sex != 'M' and c.sex != 'F')");
        }
        query.append(")");
        Query q = sessionFactory.getCurrentSession().createSQLQuery(query.toString());
        q.setCacheMode(CacheMode.IGNORE);
        return new Cohort(q.list());
    }

    @Override
    public Cohort getPatientContactWithAgeRange(Integer minAge, DurationUnit minAgeUnit, Integer maxAge, DurationUnit maxAgeUnit, boolean unknownAgeIncluded, Date effectiveDate) {

        if (effectiveDate == null) {
            effectiveDate = new Date();
        }
        if (minAgeUnit == null) {
            minAgeUnit = DurationUnit.YEARS;
        }
        if (maxAgeUnit == null) {
            maxAgeUnit = DurationUnit.YEARS;
        }

        String sql = "select c.id from kenyaemr_hiv_testing_patient_contact c where c.voided = 0 and ";
        Map<String, Date> paramsToSet = new HashMap<String, Date>();

        Date maxBirthFromAge = effectiveDate;
        if (minAge != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(effectiveDate);
            cal.add(minAgeUnit.getCalendarField(), -minAgeUnit.getFieldQuantity()*minAge);
            maxBirthFromAge = cal.getTime();
        }

        String c = "c.birth_date <= :maxBirthFromAge";
        paramsToSet.put("maxBirthFromAge", maxBirthFromAge);

        Date minBirthFromAge = null;
        if (maxAge != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(effectiveDate);
            cal.add(maxAgeUnit.getCalendarField(), -(maxAgeUnit.getFieldQuantity()*maxAge + 1));
            minBirthFromAge = cal.getTime();
            c = "(" + c + " and c.birth_date >= :minBirthFromAge)";
            paramsToSet.put("minBirthFromAge", minBirthFromAge);
        }

        if (unknownAgeIncluded) {
            c = "(c.birth_date is null or " + c + ")";
        }

        sql += c;

        log.debug("Executing: " + sql + " with params: " + paramsToSet);

        Query query = sessionFactory.getCurrentSession().createSQLQuery(sql);
        for (Map.Entry<String, Date> entry : paramsToSet.entrySet()) {
            query.setDate(entry.getKey(), entry.getValue());
        }

        return new Cohort(query.list());
    }

    @Override
    public List<LabManifestOrder> getLabManifestOrderByStatusBeforeDate(String status, Date lastStatusCheckDate) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("status", status));
        criteria.add(Restrictions.eq("voided", false));
        criteria.add(Restrictions.or(Restrictions.isNull("lastStatusCheckDate"), Restrictions.le("lastStatusCheckDate", lastStatusCheckDate)));
        criteria.addOrder(org.hibernate.criterion.Order.asc("id"));
        criteria.setMaxResults(50);
        return criteria.list();
    }

    @Override
    public Long countLabManifestOrderByStatusBeforeDate(String status, Date lastStatusCheckDate) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("status", status));
        criteria.add(Restrictions.eq("voided", false));
        criteria.add(Restrictions.or(Restrictions.isNull("lastStatusCheckDate"), Restrictions.le("lastStatusCheckDate", lastStatusCheckDate)));
        criteria.setProjection(Projections.rowCount());
        List rowCount = criteria.list();
        Long count = (Long) rowCount.get(0);
        return(count);
    }

    @Override
    public LabManifest getLabOrderManifestByStatus(String status, Date onOrBefore) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.add(Restrictions.eq("status", status));
        criteria.add(Restrictions.le("dispatchDate", onOrBefore));
        criteria.addOrder(org.hibernate.criterion.Order.asc("id"));
        criteria.add(Restrictions.eq("voided", false));
        // return the earliest - the first in the list
        if (criteria.list().size() > 0) {
            return (LabManifest) criteria.list().get(0);
        }
        return null;
    }

    @Override
    public List<LabManifest> getLabOrderManifest(String status) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.add(Restrictions.eq("status", status));
        criteria.add(Restrictions.eq("voided", false));
        criteria.addOrder(org.hibernate.criterion.Order.desc("id"));
        return criteria.list();
    }

    @Override
    public List<LabManifestOrder> getLabManifestOrderByNotFoundInLabSystem(Integer... ordersList) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.in("id", ordersList));
        criteria.add(Restrictions.eq("status", "Sent")); // samples marked as sent in the manifest but are not part of the result from lab
        criteria.add(Restrictions.eq("voided", false));
        criteria.addOrder(org.hibernate.criterion.Order.asc("id"));
        return criteria.list();
    }

    @Override
    public List<LabManifestOrder> getLabManifestOrderByManifestAndStatus(LabManifest labManifestOrder, Date updatedBefore, String... status) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.le("lastStatusCheckDate", updatedBefore));
        criteria.add(Restrictions.eq("labManifest", labManifestOrder));
        criteria.add(Restrictions.in("status", status));
        criteria.add(Restrictions.eq("voided", false));
        criteria.addOrder(org.hibernate.criterion.Order.asc("id"));
        criteria.setMaxResults(50);

        return criteria.list();
    }

    @Override
    public Long countLabManifestOrderByManifestAndStatus(LabManifest labManifestOrder, Date updatedBefore, String... status) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.le("lastStatusCheckDate", updatedBefore));
        criteria.add(Restrictions.eq("labManifest", labManifestOrder));
        criteria.add(Restrictions.in("status", status));
        criteria.add(Restrictions.eq("voided", false));
        criteria.setProjection(Projections.rowCount());
        List rowCount = criteria.list();
        Long count = (Long) rowCount.get(0);
        return(count);
    }

    @Override
    public LabManifestOrder getLabManifestOrderByOrderId(Order specimenId) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("order", specimenId));
        if (criteria.list().size() > 0) {
            return (LabManifestOrder) criteria.list().get(0);
        }
        return null;
    }

    @Override
    public LabManifest getFirstLabManifestByOrderStatusCheckedBeforeDate(String status, Date lastStatusCheckDate) {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifestOrder.class);
        criteria.add(Restrictions.eq("status", status));
        criteria.add(Restrictions.eq("voided", false));
        criteria.add(Restrictions.or(Restrictions.isNull("lastStatusCheckDate"), Restrictions.le("lastStatusCheckDate", lastStatusCheckDate)));
        criteria.addOrder(org.hibernate.criterion.Order.asc("id"));
        criteria.setMaxResults(1);

        if (criteria.list().size() > 0) {
            LabManifestOrder manifestOrder = (LabManifestOrder) criteria.list().get(0);
            return manifestOrder.getLabManifest();
        }
        return null;
    }

    /**
     * This puts the manifest in the submitted state allowing the system to check for results from remote again
     * It also changes the status of the manifest samples in error to "Sent"
     * 
     * @param manifestId - The manifest id
     */
    @Override
    public void reprocessLabManifest(Integer manifestId) {
        LabManifest labManifest = getLabOrderManifestById(manifestId);
        labManifest.setStatus("Submitted");
        saveLabOrderManifest(labManifest);
        //get all orders in manifest
        List<LabManifestOrder> ordersList = getLabManifestOrderByManifest(labManifest);
        for (LabManifestOrder labManifestOrder : ordersList) {
            // Modify the order in case the status is in error
            String status = labManifestOrder.getStatus();
            if(!status.trim().equalsIgnoreCase("Complete") && !status.trim().equalsIgnoreCase("Incomplete")) {
                labManifestOrder.setStatus("Sent");
                saveLabManifestOrder(labManifestOrder);
            }
        }
    }

    // Start cached data for summary form

    @Override
    public Long countTotalDraftManifests() {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.add(Restrictions.eq("status", "Draft"));
        criteria.add(Restrictions.eq("voided", false));
        criteria.setProjection(Projections.rowCount());
        List rowCount = criteria.list();
        Long count = (Long) rowCount.get(0);
        return(count);
    }

    @Override
    public Long countTotalManifestsOnHold() {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.add(Restrictions.eq("status", "On hold"));
        criteria.add(Restrictions.eq("voided", false));
        criteria.setProjection(Projections.rowCount());
        List rowCount = criteria.list();
        Long count = (Long) rowCount.get(0);
        return(count);
    }

    @Override
    public Long countTotalReadyToSendManifests() {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.add(Restrictions.eq("status", "Ready to send"));
        criteria.add(Restrictions.eq("voided", false));
        criteria.setProjection(Projections.rowCount());
        List rowCount = criteria.list();
        Long count = (Long) rowCount.get(0);
        return(count);
    }

    @Override
    public Long countTotalManifestsOnSending() {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.add(Restrictions.eq("status", "Sending"));
        criteria.add(Restrictions.eq("voided", false));
        criteria.setProjection(Projections.rowCount());
        List rowCount = criteria.list();
        Long count = (Long) rowCount.get(0);
        return(count);
    }

    @Override
    public Long countTotalSubmittedManifests() {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.add(Restrictions.eq("status", "Submitted"));
        criteria.add(Restrictions.eq("voided", false));
        criteria.setProjection(Projections.rowCount());
        List rowCount = criteria.list();
        Long count = (Long) rowCount.get(0);
        return(count);
    }

    @Override
    public Long countTotalIncompleteManifests() {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.add(Restrictions.eq("status", "Incomplete results"));
        criteria.add(Restrictions.eq("voided", false));
        criteria.setProjection(Projections.rowCount());
        List rowCount = criteria.list();
        Long count = (Long) rowCount.get(0);
        return(count);
    }

    @Override
    public Long countTotalManifestsIncompleteWithErrors() {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.add(Restrictions.eq("status", "Incomplete results"));
        criteria.add(Restrictions.eq("voided", false));
        criteria.setProjection(Projections.rowCount());
        List rowCount = criteria.list();
        Long count = (Long) rowCount.get(0);
        return(count);
    }

    @Override
    public Long countTotalErrorsOnIncompleteManifests() {
        Context.addProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
        String sql = "select convert(count(*), CHAR) as count from kenyaemr_order_entry_lab_manifest_order o, kenyaemr_order_entry_lab_manifest p where o.status = 'Record not found' and p.status = 'Incomplete results' and p.id = o.manifest_id;";

        Long count = 0L;
        try {
            List<List<Object>> errors = Context.getAdministrationService().executeSQL(sql, true);
            if ( errors != null && !errors.isEmpty() ) {
                for (List<Object> res : errors) {
                    try {
                        String rawCount = res.get(0) != null ? (String) res.get(0) : "0";
                        count = Long.valueOf(rawCount);
                    } catch(Exception ex) {
                        System.err.println("Get total errors on incomplete manifests: Error : " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
        } catch(Exception ex) {
            System.err.println("Error getting total errors on incomplete manifests: " + ex.getMessage());
        }
        Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);

        return(count);
    }

    @Override
    public Long countTotalCompleteManifests() {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.add(Restrictions.eq("status", "Complete results"));
        criteria.add(Restrictions.eq("voided", false));
        criteria.setProjection(Projections.rowCount());
        List rowCount = criteria.list();
        Long count = (Long) rowCount.get(0);
        return(count);
    }

    @Override
    public Long countTotalManifestsCompleteWithErrors() {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(LabManifest.class);
        criteria.add(Restrictions.eq("status", "Complete results"));
        criteria.add(Restrictions.eq("voided", false));
        criteria.setProjection(Projections.rowCount());
        List rowCount = criteria.list();
        Long count = (Long) rowCount.get(0);
        return(count);
    }

    @Override
    public Long countTotalErrorsOnCompleteManifests() {
        Context.addProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
        String sql = "select convert(count(*), CHAR) as count from kenyaemr_order_entry_lab_manifest_order o, kenyaemr_order_entry_lab_manifest p where o.status = 'Record not found' and p.status = 'Complete results' and p.id = o.manifest_id;";

        Long count = 0L;
        try {
            List<List<Object>> errors = Context.getAdministrationService().executeSQL(sql, true);
            if ( errors != null && !errors.isEmpty() ) {
                for (List<Object> res : errors) {
                    try {
                        String rawCount = res.get(0) != null ? (String) res.get(0) : "0";
                        count = Long.valueOf(rawCount);
                    } catch(Exception ex) {
                        System.err.println("Get total errors on complete manifests: Error : " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
        } catch(Exception ex) {
            System.err.println("Error getting total errors on complete manifests: " + ex.getMessage());
        }
        Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);

        return(count);
    }

}
