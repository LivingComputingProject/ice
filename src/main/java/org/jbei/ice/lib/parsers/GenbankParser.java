package org.jbei.ice.lib.parsers;

import java.io.BufferedReader;
import java.util.HashSet;
import java.util.Set;

import org.biojava.bio.BioException;
import org.biojava.bio.seq.Feature;
import org.biojavax.Note;
import org.biojavax.bio.seq.RichFeature;
import org.biojavax.bio.seq.RichLocation;
import org.biojavax.bio.seq.RichSequence;
import org.biojavax.bio.seq.RichSequenceIterator;
import org.biojavax.bio.seq.RichSequence.IOTools;
import org.jbei.ice.lib.models.Sequence;
import org.jbei.ice.lib.models.SequenceFeature;
import org.jbei.ice.lib.utils.Utils;

public class GenbankParser {
    @SuppressWarnings("unchecked")
    public static Sequence parseGenbankDNAFile(BufferedReader br) throws ParserException {
        Sequence sequence = null;

        try {
            RichSequenceIterator richSequences = IOTools.readGenbankDNA(br, null);

            if (richSequences.hasNext()) {
                RichSequence richSequence = richSequences.nextRichSequence();

                Set<Feature> featureSet = richSequence.getFeatureSet();

                Set<SequenceFeature> sequenceFeatureSet = new HashSet<SequenceFeature>();
                for (Feature feature : featureSet) {
                    RichFeature richFeature = (RichFeature) feature;

                    String featureDescription = "";
                    String featureName = "";

                    Set<Note> notes = (Set<Note>) richFeature.getNoteSet();
                    for (Note note : notes) {
                        featureDescription = note.getTerm().getName() + "=" + note.getValue();

                        if (note.getTerm().getName().toLowerCase().equals("name")
                                || note.getTerm().getName().toLowerCase().equals("label")) {
                            featureName = note.getValue();
                        }
                    }

                    RichLocation featureLocation = (RichLocation) richFeature.getLocation();
                    String genbankType = richFeature.getType();
                    int start = featureLocation.getMin();
                    int end = featureLocation.getMax();

                    SequenceFeature sequenceFeature = new SequenceFeature(sequence,
                            new org.jbei.ice.lib.models.Feature(featureName, featureDescription, "", Utils
                                    .generateUUID(), 0, genbankType), start, end, featureLocation.getStrand()
                                    .intValue(), featureName);

                    sequenceFeatureSet.add(sequenceFeature);
                }

                sequence = new Sequence(richSequence.seqString(), "", "", "", null, sequenceFeatureSet);
            }
        } catch (BioException e) {
            throw new ParserException("BioJava Exception. ", e);
        } catch (Exception e) {
            throw new ParserException(e);
        }

        return sequence;
    }
}