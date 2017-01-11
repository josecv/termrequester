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
package org.phenotips.termrequester.github;

/**
 * A github issue label class. Example:
 * {
 *    "id": 516184566,
 *    "url": "https://api.github.com/repos/phenotips/termrequester/labels/acceptedautorequest",
 *    "name": "acceptedautorequest",
 *    "color": "c2e0c6",
 *    "default": false
 *  }
 *
 * @version $Id$
 */
public class Label
{
    /**
     * The issue label id.
     */
    private int id;

    /**
     * The issue label url.
     */
    private String url;

    /**
     * The issue label name.
     */
    private String name;

    /**
     * The issue label color.
     */
    private String color;

    /**
     * Is this issue label a default one.
     */
    private boolean isDefault;

    /**
     * Get id.
     *
     * @return id as int.
     */
    public int getId()
    {
        return id;
    }

    /**
     * Set id.
     *
     * @param id the value to set.
     */
    public void setId(int id)
    {
        this.id = id;
    }

    /**
     * Get url.
     *
     * @return url as String.
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * Set url.
     *
     * @param url the value to set.
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * Get name.
     *
     * @return name as String.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set name.
     *
     * @param name the value to set.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Get color.
     *
     * @return color as String.
     */
    public String getColor()
    {
        return color;
    }

    /**
     * Set color.
     *
     * @param color the value to set.
     */
    public void setColor(String color)
    {
        this.color = color;
    }

    /**
     * Get the label default status.
     *
     * @return true if the label is a default one
     */
    public boolean isDefault()
    {
        return isDefault;
    }

    /**
     * Set the label default status.
     *
     * @param isDefault default status
     */
    public void setDefault(boolean isDefault)
    {
        this.isDefault = isDefault;
    }
}
