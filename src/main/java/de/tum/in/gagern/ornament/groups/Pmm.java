/* $Id: Pmm.java,v 1.4 2003/02/16 23:43:15 gagern Exp $

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
 * The crystallographic group pmm.
 *
 * @author Martin von Gagern
 * @version $Revision: 1.4 $
 */
public class Pmm extends Group implements Constants {

    static final String[] NAMES={"pmm", "*2222"};

    static final double[][] TRANSFORMS={
	{1, 0, 0, 1, 0, 0}, {-1, 0, 0, 1, 1, 0},
	{1, 0, 0, -1, 0, 1}, {-1, 0, 0, -1, 1, 1}
    };

    static final Property[] PROPERTIES={
	new Property.Turn2(0, 0, 1),
	new Property.Turn2(0, .5, 2),
	new Property.Turn2(.5, 0, 3),
	new Property.Turn2(.5, .5, 4),
	new Property.Mirror(0, 0, 1, 0, 1),
	new Property.Mirror(0, .5, 1, .5, 2),
	new Property.Mirror(0, 0, 0, 1, 3),
	new Property.Mirror(.5, 0, .5, 1, 4)
    };

    private static final Object[] SNAP_VORONOI = null;

    public Pmm() {
	super(NAMES, TRANSFORMS, PROPERTIES, SNAP_VORONOI);
    }

    public int getTileType() {
	return TILETYPE_RECTANGLE;
    }

}
