package com.example.application.views;

//import com.example.application.data.service.CrmService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

@PermitAll
@Route(value = "dashboard", layout = MainLayout.class) // <1>
@PageTitle("Dashboard | Vaadin CRM")
public class DashboardView extends VerticalLayout {
  //  private final CrmService service;

    public DashboardView(/*CrmService service*/) { // <2>
    //    this.service = service;
        addClassName("dashboard-view");
        setDefaultHorizontalComponentAlignment(Alignment.CENTER); // <3>
        add(new Label("Hello World!"));
       // add(getContactStats(), getCompaniesChart());
    }

   
}