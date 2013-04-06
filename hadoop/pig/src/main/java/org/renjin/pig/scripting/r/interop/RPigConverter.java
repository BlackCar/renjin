/*
 * R : A Computer Language for Statistical Data Analysis
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1997--2008  The R Development Core Team
 * Copyright (C) 2003, 2004  The R Foundation
 * Copyright (C) 2010 bedatadriven
 * Copyright (C) 2012 The Renjin project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.renjin.pig.scripting.r.interop;

import java.util.Map;

import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DefaultBagFactory;
import org.apache.pig.data.TupleFactory;
import org.renjin.sexp.DoubleVector;
import org.renjin.sexp.HasNamedValues;
import org.renjin.sexp.ListVector;
import org.renjin.sexp.NamedValue;
import org.renjin.sexp.SEXP;

import static com.google.common.collect.Maps.newHashMap;

/**
 * Helpers to convert to and from R
 */
public class RPigConverter {

    private static TupleFactory tupleFactory = TupleFactory.getInstance();
    private static BagFactory bagFactory = DefaultBagFactory.getInstance();

    public static Object convertRToPig(SEXP sexp) {
        if (sexp instanceof HasNamedValues) {
            Iterable<NamedValue> values = ((HasNamedValues)sexp).namedValues();
            Map toReturn = newHashMap();

            for (NamedValue value : values) {
                toReturn.put(value.getName(), convertRToPig(value.getValue()));
            }
            return toReturn;
        } else if (sexp instanceof ListVector) {

        } else if (sexp instanceof DoubleVector) {

        } else {
            throw new IllegalArgumentException("Unknown R type to convert: " + sexp.getClass().getCanonicalName());
        }
        return null;
    }

}
