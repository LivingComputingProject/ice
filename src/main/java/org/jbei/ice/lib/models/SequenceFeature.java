package org.jbei.ice.lib.models;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;

import org.jbei.ice.lib.dao.IModel;

import org.hibernate.annotations.Cascade;

import org.hibernate.annotations.Type;

/**
 * Stores the sequence annotation information, and associates {@link Feature} objects to a
 * {@link Sequence} object.
 * <p/>
 * SequenceFeature represents is a many-to-many mapping. In addition, this class has fields to store
 * sequence specific annotation information.
 *
 * @author Timothy Ham, Zinovii Dmytriv
 */
@Entity
@Table(name = "sequence_feature")
@SequenceGenerator(name = "sequence", sequenceName = "sequence_feature_id_seq", allocationSize = 1)
public class SequenceFeature implements IModel {

    public static final String DESCRIPTION = "description";

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence")
    private long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sequence_id")
    private Sequence sequence;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "feature_id")
    private Feature feature;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "sequenceFeature")
    @OrderBy("id")
    private final Set<AnnotationLocation> annotationLocations = new LinkedHashSet<AnnotationLocation>();

    @Deprecated
    @Column(name = "feature_start")
    private int genbankStart;

    @Deprecated
    @Column(name = "feature_end")
    private int end;

    /**
     * +1 or -1
     */
    @Column(name = "strand")
    private int strand;

    @Column(name = "name", length = 127)
    private String name;

    /**
     * Deprecated since schema 0.8.0. Use SequenceFeatureAttribute with "description" as key
     */
    @Deprecated
    @Column(name = "description")
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String description;

    @Column(name = "genbank_type", length = 127)
    private String genbankType;

    @Column(name = "flag")
    @Enumerated(EnumType.STRING)
    private AnnotationType annotationType;

    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER, mappedBy = "sequenceFeature")
    @JoinColumn(name = "sequence_feature_id")
    @OrderBy("id")
    private final Set<SequenceFeatureAttribute> sequenceFeatureAttributes = new
            LinkedHashSet<SequenceFeatureAttribute>();

    public SequenceFeature() {
        super();
    }

    public SequenceFeature(Sequence sequence, Feature feature, int strand, String name,
            String genbankType, AnnotationType annotationType) {
        super();
        this.sequence = sequence;
        this.feature = feature;
        this.strand = strand;
        this.name = name;
        this.genbankType = genbankType;
        this.annotationType = annotationType;
    }

    /**
     * Annotation type for "parts".
     * <p/>
     * Parts can have a PREFIX, a SUFFIX, a SCAR features. The INNER and SUBINNER features indicate
     * part sequence excluding the prefix and suffix.
     *
     * @author Timothy Ham
     */
    public enum AnnotationType {
        PREFIX, SUFFIX, SCAR, INNER, SUBINNER;
    }

    public void setId(long id) {
        this.id = id;
    }

    @XmlTransient
    public long getId() {
        return id;
    }

    @XmlTransient
    public Sequence getSequence() {
        return sequence;
    }

    public void setSequence(Sequence sequence) {
        this.sequence = sequence;
    }

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    public void setAnnotationLocations(Set<AnnotationLocation> annotationLocations) {
        // for JAXB web services
        if (annotationLocations == null) {
            this.annotationLocations.clear();
            return;
        }
        if (annotationLocations != this.annotationLocations) {
            annotationLocations.clear();
            this.annotationLocations.addAll(annotationLocations);
        }
    }

    public Set<AnnotationLocation> getAnnotationLocations() {
        return annotationLocations;
    }

    /**
     * Use locations instead. This field exists to allow scripted migration of data using
     * the new database schema.
     */
    @Deprecated
    public int getGenbankStart() {
        return genbankStart;
    }

    /**
     * Use locations instead. This field exists to allow scripted migration of data using
     * the new database schema.
     */
    @Deprecated
    public void setGenbankStart(int genbankStart) {
        this.genbankStart = genbankStart;
    }

    /**
     * Use locations instead. This field exists to allow scripted migration of data using
     * the new database schema.
     */
    @Deprecated
    public int getEnd() {
        return end;
    }

    /**
     * Use locations instead. This field exists to allow scripted migration of data using
     * the new database schema.
     */
    @Deprecated
    public void setEnd(int end) {
        this.end = end;
    }

    public int getStrand() {
        return strand;
    }

    /**
     * +1 for forward, -1 for reverse.
     */
    public void setStrand(int strand) {
        this.strand = strand;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGenbankType() {
        return genbankType;
    }

    public void setGenbankType(String genbankType) {
        this.genbankType = genbankType;
    }

    public void setAnnotationType(AnnotationType annotationType) {
        this.annotationType = annotationType;
    }

    public AnnotationType getAnnotationType() {
        return annotationType;
    }

    public Set<SequenceFeatureAttribute> getSequenceFeatureAttributes() {
        return sequenceFeatureAttributes;
    }

    public void setSequenceFeatureAttributes(Set<SequenceFeatureAttribute> sequenceFeatureAttributes) {
        if (sequenceFeatureAttributes == null) {
            this.sequenceFeatureAttributes.clear();
            return;
        }

        if (this.sequenceFeatureAttributes != sequenceFeatureAttributes) {
            sequenceFeatureAttributes.clear();
            sequenceFeatureAttributes.addAll(sequenceFeatureAttributes);
        }

    }

    public Integer getUniqueGenbankStart() {
        Integer result = null;
        if (getAnnotationLocations() != null && getAnnotationLocations().size() == 1) {
            result = ((AnnotationLocation) getAnnotationLocations().toArray()[0]).getGenbankStart();
        }
        return result;
    }

    public Integer getUniqueEnd() {
        Integer result = null;
        if (getAnnotationLocations() != null && getAnnotationLocations().size() == 1) {
            result = ((AnnotationLocation) getAnnotationLocations().toArray()[getAnnotationLocations()
                    .size() - 1]).getEnd();
        }
        return result;
    }

}
