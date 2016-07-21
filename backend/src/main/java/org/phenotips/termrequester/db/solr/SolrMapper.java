/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.termrequester.db.solr;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import org.phenotips.termrequester.HPOPhenotype;
import org.phenotips.termrequester.Phenotype;

import com.google.common.base.Joiner;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Maps a Phenotype object to a solr doc (and back).
 *
 * @version $Id$
 */
class SolrMapper
{
    /**
     * Convert the Phenotype given to a solr document.
     * @param pt the phenotype
     * @return a document
     */
    public SolrInputDocument toDoc(Phenotype pt)
    {
        checkArgument(pt.getId().isPresent(), "Missing id for " + pt);
        Set<String> synonyms = pt.getSynonyms();
        Date now = new Date();
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField(Schema.NAME, pt.getName());
        doc.setField(Schema.DEFINITION, pt.getDescription());
        doc.setField(Schema.STATUS, pt.getStatus().name());
        doc.setField(Schema.ISSUE_NUMBER, pt.getIssueNumber().or("0"));
        doc.setField(Schema.PARENT, pt.getParent().getId().or("0"));
        doc.setField(Schema.SYNONYM, synonyms.toArray(new String[synonyms.size()]));
        doc.setField(Schema.ID, pt.getId().get());
        if (pt.getHpoId().isPresent()) {
            doc.setField(Schema.HPO_ID, pt.getHpoId().get());
        }
        if (pt.getTimeCreated().isPresent()) {
            doc.setField(Schema.TIME_CREATED, pt.getTimeCreated().get());
        } else {
            doc.setField(Schema.TIME_CREATED, now);
        }
        doc.setField(Schema.TIME_MODIFIED, now);
        return doc;
    }

    /**
     * Turn the document given into a Phenotype instance.
     * Note that the parent will not be populated.
     * @param doc the document
     * @return the instance
     */
    public Phenotype fromDoc(SolrDocument doc)
    {
        String name = (String) doc.getFieldValue(Schema.NAME);
        String description = (String) doc.getFieldValue(Schema.DEFINITION);
        Phenotype.Status status = Phenotype.Status.valueOf((String) doc.getFieldValue(Schema.STATUS));
        Phenotype pt;
        if (status == Phenotype.Status.ACCEPTED) {
            pt = new HPOPhenotype(name, description);
            pt.setHpoId((String) doc.getFieldValue(Schema.HPO_ID));
        } else {
            pt = new Phenotype(name, description);
        }
        pt.setStatus(status);
        pt.setId((String) doc.getFieldValue(Schema.ID));
        Collection<Object> synonyms = doc.getFieldValues(Schema.SYNONYM);
        if (synonyms != null) {
            for (Object synonym : synonyms) {
                pt.addSynonym((String) synonym);
            }
        }
        pt.setIssueNumber((String) doc.getFieldValue(Schema.ISSUE_NUMBER));
        pt.setTimeCreated((Date) doc.getFieldValue(Schema.TIME_CREATED));
        pt.setTimeModified((Date) doc.getFieldValue(Schema.TIME_MODIFIED));
        return pt;
    }
}
