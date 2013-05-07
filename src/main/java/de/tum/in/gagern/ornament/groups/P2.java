/* $Id: P2.java,v 1.4 2003/02/17 13:27:12 gagern Exp $

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

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import de.tum.in.gagern.ornament.*;

/**
 * The crystallographic group p2.
 *
 * @author Martin von Gagern
 * @version $Revision: 1.4 $
 */
public class P2 extends Group implements Constants {

    static final String[] NAMES={"p2", "2222"};

    static final double[][] TRANSFORMS={
	{1, 0, 0, 1, 0, 0}, {-1, 0, 0, -1, 1, 1}
    };

    static final Property[] PROPERTIES={
	new Property.Turn2(0, 0, 1),
	new Property.Turn2(0, .5, 2),
	new Property.Turn2(.5, 0, 3),
	new Property.Turn2(.5, .5, 4)
    };

    static final Object[] SNAP_VORONOI={
	new Point2D.Double(.25, .25),
	new Point2D.Double(.25, .5),
	new Point2D.Double(.25, .75),
	new Point2D.Double(.5, .25),
	new Point2D.Double(.5, .75),
	new Point2D.Double(.75, .25),
	new Point2D.Double(.75, .5),
	new Point2D.Double(.75, .75),
    };

    public P2() {
	super(NAMES, TRANSFORMS, PROPERTIES, SNAP_VORONOI);
    }

    public int getTileType() {
	return TILETYPE_PARALLEL;
    }

}
