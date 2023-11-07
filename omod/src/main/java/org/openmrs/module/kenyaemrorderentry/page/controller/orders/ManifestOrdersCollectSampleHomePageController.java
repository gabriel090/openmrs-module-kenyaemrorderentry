package org.openmrs.module.kenyaemrorderentry.page.controller.orders;

import java.util.ArrayList;
import java.util.List;

import org.openmrs.Concept;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrorderentry.api.service.KenyaemrOrdersService;
import org.openmrs.module.kenyaemrorderentry.manifest.LabManifest;
import org.openmrs.module.kenyaemrorderentry.manifest.LabManifestOrder;
import org.openmrs.module.kenyaemrorderentry.metadata.KenyaemrorderentryAdminSecurityMetadata;
import org.openmrs.module.kenyaemrorderentry.util.Utils;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.parameter.OrderSearchCriteriaBuilder;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

@AppPage("kenyaemr.labmanifest")
public class ManifestOrdersCollectSampleHomePageController {

    public void get(@RequestParam(value = "manifest", required = false) LabManifest manifest, @SpringBean KenyaUiUtils kenyaUi,
                    UiUtils ui, PageModel model) {

        // Is DOD
        AdministrationService administrationService = Context.getAdministrationService();
        final String isKDoD = (administrationService.getGlobalProperty("kenyaemr.isKDoD"));

        List<LabManifestOrder> allOrders = Context.getService(KenyaemrOrdersService.class).getLabManifestOrderByStatus("Collect New Sample");
        PatientIdentifierType pat = Utils.getUniquePatientNumberIdentifierType();
        PatientIdentifierType kat = Utils.getKDODIdentifierType();
        PatientIdentifierType hei = Utils.getHeiNumberIdentifierType();
        OrderService orderService = Context.getOrderService();
        List<LabManifestOrder> filteredOrders = new ArrayList<LabManifestOrder>();
        OrderType labOrderType = orderService.getOrderTypeByUuid(OrderType.TEST_ORDER_TYPE_UUID);
        List<OrderType> orderTypes = new ArrayList<OrderType>();
        orderTypes.add(labOrderType);

        for (LabManifestOrder o : allOrders) {
            OrderSearchCriteriaBuilder builder = new OrderSearchCriteriaBuilder();
            builder.setActivatedOnOrAfterDate(o.getOrder().getDateActivated());
            builder.setPatient(o.getOrder().getPatient());
            builder.setOrderTypes(orderTypes);
            List<Concept> vlConcepts = new ArrayList<Concept>();
            vlConcepts.add(Context.getConceptService().getConcept(856));
            vlConcepts.add(Context.getConceptService().getConcept(1405));
            builder.setConcepts(vlConcepts);
            List<Order> orders = orderService.getOrders(builder.build());
            if (orders.size() < 1) {
                filteredOrders.add(o);
            }

        }

        model.put("sampleList", filteredOrders);
        model.put("sampleListSize", filteredOrders.size());
        model.put("cccNumberType", "");
        model.put("heiNumberType", "");

        if(isKDoD.trim().equalsIgnoreCase("true")) {
            model.put("cccNumberType", kat.getPatientIdentifierTypeId());
            model.put("heiNumberType", kat.getPatientIdentifierTypeId());
        } else {
            model.put("cccNumberType", pat.getPatientIdentifierTypeId());
            model.put("heiNumberType", hei.getPatientIdentifierTypeId());
        }

        model.put("userHasSettingsEditRole", (Context.getAuthenticatedUser().containsRole(KenyaemrorderentryAdminSecurityMetadata._Role.API_ROLE_EDIT_SETTINGS) || Context.getAuthenticatedUser().isSuperUser()));
    }

}