/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.parser;

import java.io.IOException;
import org.apache.batik.parser.ParseException;

/**
 * This class represents a parser with support for numbers.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id$
 */
public abstract class NumberParser extends AbstractParser {

    /**
     * Parses the content of the buffer and converts it to a float.
     */
    protected float parseFloat() throws ParseException {
        int     mant     = 0;
        int     mantDig  = 0;
        boolean mantPos  = true;
        boolean mantRead = false;

        int     exp      = 0;
        int     expDig   = 0;
        int     expAdj   = 0;
        boolean expPos   = true;

        switch (current) {
        case '-':
            mantPos = false;
        case '+':
            if (position == count && !fillBuffer()) {
                current = -1;
                break;
            }
            current = buffer[position++];
            column++;
        }

        m1: switch (current) {
        case 10:
            line++;
            column = 1;
        default:
            reportError("character.unexpected",
                        new Object[] { new Integer(current) });
            return 0f;

        case '.':
            break;

        case '0':
            mantRead = true;
            l: for (;;) {
                if (position == count && !fillBuffer()) {
                    current = -1;
                } else {
                    current = buffer[position++];
                    column++;
                }
                switch (current) {
                case '1': case '2': case '3': case '4': 
                case '5': case '6': case '7': case '8': case '9': 
                    break l;
                case '.': case 'e': case 'E':
                    break m1;
                case 10:
                    line++;
                    column = 1;
                default:
                    return 0f;
                case '0':
                }
            }

        case '1': case '2': case '3': case '4': 
        case '5': case '6': case '7': case '8': case '9': 
            mantRead = true;
            l: for (;;) {
                if (mantDig < 9) {
                    mantDig++;
                    mant = mant * 10 + (current - '0');
                } else {
                    expAdj++;
                }
                if (position == count && !fillBuffer()) {
                    current = -1;
                } else {
                    current = buffer[position++];
                    column++;
                }
                switch (current) {
                case 10:
                    line++;
                    column = 1;
                default:
                    break l;
                case '0': case '1': case '2': case '3': case '4': 
                case '5': case '6': case '7': case '8': case '9': 
                }                
            }
        }
        
        if (current == '.') {
            if (position == count && !fillBuffer()) {
                current = -1;
            } else {
                current = buffer[position++];
                column++;
            }
            m2: switch (current) {
            case 10:
                line++;
                column = 1;
            default:
            case 'e': case 'E':
                if (!mantRead) {
                    reportError("character.unexpected",
                                new Object[] { new Integer(current) });
                    return 0f;
                }
                break;

            case '0':
                if (mantDig == 0) {
                    l: for (;;) {
                        if (position == count && !fillBuffer()) {
                            current = -1;
                        } else {
                            current = buffer[position++];
                            column++;
                        }
                        expAdj--;
                        switch (current) {
                        case '1': case '2': case '3': case '4': 
                        case '5': case '6': case '7': case '8': case '9': 
                            break l;
                        case 10:
                            line++;
                            column = 1;
                        default:
                            if (!mantRead) {
                                return 0f;
                            }
                            break m2;
                        case '0':
                        }
                    }
                }
            case '1': case '2': case '3': case '4': 
            case '5': case '6': case '7': case '8': case '9': 
                l: for (;;) {
                    if (mantDig < 9) {
                        mantDig++;
                        mant = mant * 10 + (current - '0');
                        expAdj--;
                    }
                    if (position == count && !fillBuffer()) {
                        current = -1;
                    } else {
                        current = buffer[position++];
                        column++;
                    }
                    switch (current) {
                    case 10:
                        line++;
                        column = 1;
                    default:
                        break l;
                    case '0': case '1': case '2': case '3': case '4': 
                    case '5': case '6': case '7': case '8': case '9': 
                    }
                }
            }
        }

        switch (current) {
        case 'e': case 'E':
            if (position == count && !fillBuffer()) {
                current = -1;
            } else {
                current = buffer[position++];
                column++;
            }
            switch (current) {
            case 10:
                line++;
                column = 1;
            default:
                reportError("character.unexpected",
                            new Object[] { new Integer(current) });
                return 0f;
            case '-':
                expPos = false;
            case '+':
                if (position == count && !fillBuffer()) {
                    current = -1;
                } else {
                    current = buffer[position++];
                    column++;
                }
                switch (current) {
                case 10:
                    line++;
                    column = 1;
                default:
                    reportError("character.unexpected",
                                new Object[] { new Integer(current) });
                    return 0f;
                case '0': case '1': case '2': case '3': case '4': 
                case '5': case '6': case '7': case '8': case '9': 
                }
            case '0': case '1': case '2': case '3': case '4': 
            case '5': case '6': case '7': case '8': case '9': 
            }
            
            en: switch (current) {
            case '0':
                l: for (;;) {
                    if (position == count && !fillBuffer()) {
                        current = -1;
                    } else {
                        current = buffer[position++];
                        column++;
                    }
                    switch (current) {
                    case '1': case '2': case '3': case '4': 
                    case '5': case '6': case '7': case '8': case '9': 
                        break l;
                    case 10:
                        line++;
                        column = 1;
                    default:
                        break en;
                    case '0':
                    }
                }

            case '1': case '2': case '3': case '4': 
            case '5': case '6': case '7': case '8': case '9': 
                l: for (;;) {
                    if (expDig < 3) {
                        expDig++;
                        exp = exp * 10 + (current - '0');
                    }
                    if (position == count && !fillBuffer()) {
                        current = -1;
                    } else {
                        current = buffer[position++];
                        column++;
                    }
                    switch (current) {
                    case 10:
                        line++;
                        column = 1;
                    default:
                        break l;
                    case '0': case '1': case '2': case '3': case '4': 
                    case '5': case '6': case '7': case '8': case '9': 
                    }
                }
            }
        default:
        }

        if (!expPos) {
            exp = -exp;
        }
        exp += expAdj;
        if (!mantPos) {
            mant = -mant;
        }

        return buildFloat(mant, exp);
    }

    public static float buildFloat(int mant, int exp) {
        if ((exp < -125) || (mant==0)) return 0f;
        if (exp >  128) {
            if (mant > 0) return Float.POSITIVE_INFINITY;
            else          return Float.NEGATIVE_INFINITY;
        }

        if (exp == 0) return mant;
            
        if (mant >= 1<<26)
            mant++;  // round up trailing bits if they will be dropped.

        if (exp >  0) return mant*pow10[exp];
        else          return mant/pow10[-exp];
    }

    /**
     * Array of powers of ten.
     */
    private static final float pow10[] = new float [128];
    static {
      for (int i=0; i<pow10.length; i++) {
        pow10[i] = (float)Math.pow(10, i);
      }
    };
}
