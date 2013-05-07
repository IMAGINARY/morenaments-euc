/* $Id: P3m1.java,v 1.5 2003/02/17 13:27:12 gagern Exp $

Ornament 1.2 Copyright (C) 2003-2006 Martin von Gagern <Martin.vGagern@gmx.net>
Ornament drawing application/applet using crystallographic groups

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

 */

package de.tum.in.gagern.ornament.groups;

import de.tum.in.gagern.ornament.*;

/**
 * The crystallographic group p3m1.
 *
 * @author Martin von Gagern
 * @version $Revision: 1.5 $
 */
public class P3m1 extends Group implements Constants {

    static final String[] NAMES={"p3m1", "*333"};

    static final double[][] TRANSFORMS={
	{1, 0, 0, 1, 0, 0}, {0, 1, 1, 0, 0, 0},
	{-1, 1, -1, 0, 1, 0}, {-1, 0, -1, 1, 1, 0},
	{0, -1, 1, -1, 0, 1}, {1, -1, 0, -1, 0, 1}
    };

    static final Property[] PROPERTIES={
	new Property.Turn3(0, 0, 1),
	new Property.Turn3(1/3., 1/3., 2),
	new Property.Turn3(2/3., 2/3., 3),
	new Property.Mirror(0, 0, 1, 1),
	new Property.Mirror(0, 1, .5, 0),
	new Property.Mirror(0, 1, 1, .5),
	new Property.Mirror(1, 0, 0, .5),
	new Property.Mirror(1, 0, .5, 1),
	new Property.Glide(0, .25, .5, 0),
	new Property.Glide(0, .75, 1, .25),
	new Property.Glide(.5, 1, 1, .75),
	new Property.Glide(0, .5, .5, 1),
	new Property.Glide(.5, 0, 1, .5),
	new Property.Glide(.25, 0, 0, .5),
	new Property.Glide(.75, 0, .25, 1),
	new Property.Glide(1, .5, .75, 1)
    };

    private static final Object[] SNAP_VORONOI = null;

    public P3m1() {
	super(NAMES, TRANSFORMS, PROPERTIES, SNAP_VORONOI);
    }

    public int getTileType() {
	return TILETYPE_TRIANGLE;
    }

}
