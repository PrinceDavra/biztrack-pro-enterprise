package com.biztrackpro.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A single customer order (one row groups all of its line items).
 * The {@code sku} column stores the first paid line item's SKU so that COGS
 * matching can do an SKU-exact match before falling back to product name.
 */
@Entity
@Table(name = "orders",
        uniqueConstraints = @UniqueConstraint(name = "uq_order_tenant", columnNames = {"order_id", "tenant_id"}),
        indexes = {@Index(name = "idx_orders_tenant", columnList = "tenant_id"),
                   @Index(name = "idx_orders_date", columnList = "date")})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "order_id", length = 50)
    private String orderId;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "product", columnDefinition = "TEXT")
    private String product;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "qty")
    private Integer qty;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "cogs_per_unit", precision = 10, scale = 2)
    private BigDecimal cogsPerUnit = BigDecimal.ZERO;

    @Column(name = "revenue", precision = 10, scale = 2)
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(name = "refund", precision = 10, scale = 2)
    private BigDecimal refund = BigDecimal.ZERO;

    @Column(name = "profit", precision = 10, scale = 2)
    private BigDecimal profit = BigDecimal.ZERO;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "shipping_city", length = 100)
    private String shippingCity;

    @Column(name = "shipping_province", length = 100)
    private String shippingProvince;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "free_items")
    private Integer freeItems = 0;

    @Column(name = "item_count")
    private Integer itemCount = 1;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getCogsPerUnit() {
        return cogsPerUnit;
    }

    public void setCogsPerUnit(BigDecimal cogsPerUnit) {
        this.cogsPerUnit = cogsPerUnit;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getRefund() {
        return refund;
    }

    public void setRefund(BigDecimal refund) {
        this.refund = refund;
    }

    public BigDecimal getProfit() {
        return profit;
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public void setShippingCity(String shippingCity) {
        this.shippingCity = shippingCity;
    }

    public String getShippingProvince() {
        return shippingProvince;
    }

    public void setShippingProvince(String shippingProvince) {
        this.shippingProvince = shippingProvince;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getFreeItems() {
        return freeItems;
    }

    public void setFreeItems(Integer freeItems) {
        this.freeItems = freeItems;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }
}
