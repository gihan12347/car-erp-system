package com.carsale.erp.preparationpipeline.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Setter
@Getter
@Entity
@Table(name = "inspection_items")
public class InspectionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_key", nullable = false, length = 80, unique = true)
    private String itemKey;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

}
