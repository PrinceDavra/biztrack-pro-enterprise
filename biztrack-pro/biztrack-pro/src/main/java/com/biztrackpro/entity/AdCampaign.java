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

@Entity
@Table(name = "ad_campaigns",
        indexes = {@Index(name = "idx_ads_tenant", columnList = "tenant_id"),
                   @Index(name = "idx_ads_date", columnList = "date")})
public class AdCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "platform", length = 100)
    private String platform;

    @Column(name = "spend", precision = 10, scale = 2)
    private BigDecimal spend = BigDecimal.ZERO;

    @Column(name = "revenue", precision = 10, scale = 2)
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(name = "roas", precision = 8, scale = 4)
    private BigDecimal roas;

    @Column(name = "clicks")
    private Integer clicks = 0;

    @Column(name = "conversions")
    private Integer conversions = 0;

    @Column(name = "cpc", precision = 8, scale = 2)
    private BigDecimal cpc;

    @Column(name = "cpl", precision = 8, scale = 2)
    private BigDecimal cpl;

    @Column(name = "impressions")
    private Integer impressions = 0;

    @Column(name = "reach")
    private Integer reach = 0;

    @Column(name = "cpm", precision = 8, scale = 2)
    private BigDecimal cpm;

    @Column(name = "optimised_for", length = 50)
    private String optimisedFor;

    @Column(name = "roas_available")
    private Boolean roasAvailable = Boolean.FALSE;

    @Column(name = "delivery_status", length = 50)
    private String deliveryStatus;

    @Column(name = "source", length = 50)
    private String source;

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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public BigDecimal getSpend() {
        return spend;
    }

    public void setSpend(BigDecimal spend) {
        this.spend = spend;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getRoas() {
        return roas;
    }

    public void setRoas(BigDecimal roas) {
        this.roas = roas;
    }

    public Integer getClicks() {
        return clicks;
    }

    public void setClicks(Integer clicks) {
        this.clicks = clicks;
    }

    public Integer getConversions() {
        return conversions;
    }

    public void setConversions(Integer conversions) {
        this.conversions = conversions;
    }

    public BigDecimal getCpc() {
        return cpc;
    }

    public void setCpc(BigDecimal cpc) {
        this.cpc = cpc;
    }

    public BigDecimal getCpl() {
        return cpl;
    }

    public void setCpl(BigDecimal cpl) {
        this.cpl = cpl;
    }

    public Integer getImpressions() {
        return impressions;
    }

    public void setImpressions(Integer impressions) {
        this.impressions = impressions;
    }

    public Integer getReach() {
        return reach;
    }

    public void setReach(Integer reach) {
        this.reach = reach;
    }

    public BigDecimal getCpm() {
        return cpm;
    }

    public void setCpm(BigDecimal cpm) {
        this.cpm = cpm;
    }

    public String getOptimisedFor() {
        return optimisedFor;
    }

    public void setOptimisedFor(String optimisedFor) {
        this.optimisedFor = optimisedFor;
    }

    public Boolean getRoasAvailable() {
        return roasAvailable;
    }

    public void setRoasAvailable(Boolean roasAvailable) {
        this.roasAvailable = roasAvailable;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
