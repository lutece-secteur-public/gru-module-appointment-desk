/*
 * Copyright (c) 2002-2022, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.appointment.modules.desk.util;

import java.time.LocalDate;

/**
 * This is the business class for the object IncrementSlot
 */
public class IncrementSlot
{
    // Variables declarations
    private int _nIncrementingValue;
    private int _nIdForm;
    private LocalDate _startingDate;
    private LocalDate _endingDate;
    private String _strStartingTime;
    private String _strEndingTime;
    private IncrementingType _type;

    /**
     * Returns the IncrementingValue
     * 
     * @return The IncrementingValue
     */
    public int getIncrementingValue( )
    {
        return _nIncrementingValue;
    }

    /**
     * Sets the IncrementingValue
     * 
     * @param nIncrementingValue
     *            The IncrementingValue
     */
    public void setIncrementingValue( int nIncrementingValue )
    {
        _nIncrementingValue = nIncrementingValue;
    }

    /**
     * Returns the IdForm
     * 
     * @return The IdForm
     */
    public int getIdForm( )
    {
        return _nIdForm;
    }

    /**
     * Sets the IdForm
     * 
     * @param nIdForm
     *            The IdForm
     */
    public void setIdForm( int nIdForm )
    {
        _nIdForm = nIdForm;
    }

    /**
     * Returns the StartingDate
     * 
     * @return The StartingDate
     */
    public LocalDate getStartingDate( )
    {
        return _startingDate;
    }

    /**
     * Sets the StartingDate
     * 
     * @param startingDate
     *            The StartingDate
     */
    public void setStartingDate( LocalDate startingDate )
    {
        _startingDate = startingDate;
    }

    /**
     * Returns the EndingDate
     * 
     * @return The EndingDate
     */
    public LocalDate getEndingDate( )
    {
        return _endingDate;
    }

    /**
     * Sets the EndingDate
     * 
     * @param endingDate
     *            The EndingDate
     */
    public void setEndingDate( LocalDate endingDate )
    {
        _endingDate = endingDate;
    }

    /**
     * Returns the StartingTime
     * 
     * @return The StartingTime
     */
    public String getStartingTime( )
    {
        return _strStartingTime;
    }

    /**
     * Sets the StartingTime
     * 
     * @param strStartingTime
     *            The StartingTime
     */
    public void setStartingTime( String strStartingTime )
    {
        _strStartingTime = strStartingTime;
    }

    /**
     * Returns the EndingTime
     * 
     * @return The EndingTime
     */
    public String getEndingTime( )
    {
        return _strEndingTime;
    }

    /**
     * Sets the EndingTime
     * 
     * @param strEndingTime
     *            The EndingTime
     */
    public void setEndingTime( String strEndingTime )
    {
        _strEndingTime = strEndingTime;
    }

    /**
     * Returns the Type
     * 
     * @return The Type
     */
    public IncrementingType getType( )
    {
        return _type;
    }

    /**
     * Sets the Type
     * 
     * @param type
     *            The Type
     */
    public void setType( IncrementingType type )
    {
        _type = type;
    }
}
