/* $Id: Pgg.java,v 1.7 2003/02/17 23:01:31 gagern Exp $

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
 * The crystallographic group pgg.
 *
 * @author Martin von Gagern
 * @version $Revision: 1.7 $
 */
public class Pgg extends Group implements Constants {

    static final String[] NAMES={"pgg", "22x"};

    static final double[][] TRANSFORMS={
	{1, 0, 0, 1, 0, 0}, {-1, 0, 0, 1, .5, .5},
	{1, 0, 0, -1, .5, .5}, {-1, 0, 0, -1, 1, 1}
    };

    static final Property[] PROPERTIES={
	new Property.Turn2(0, 0, 1),
	new Property.Turn2(.5, .5, 1),
	new Property.Turn2(0, .5, 2),
	new Property.Turn2(.5, 0, 2),
	new Property.Glide(0, .25, 1, .25, 1),
	new Property.Glide(0, .75, 1, .75, 1),
	new Property.Glide(.25, 0, .25, 1, 2),
	new Property.Glide(.75, 0, .75, 1, 2)
    };

    static final Object[] SNAP_VORONOI={
	new Point2D.Double(.25, .25),
	new Point2D.Double(.75, .25),
	new Point2D.Double(.25, .75),
	new Point2D.Double(.75, .75),
	new Point2D.Double(1/8., .25),
	new Point2D.Double(3/8., .25),
	new Point2D.Double(5/8., .25),
	new Point2D.Double(7/8., .25),
	new Point2D.Double(1/8., .75),
	new Point2D.Double(3/8., .75),
	new Point2D.Double(5/8., .75),
	new Point2D.Double(7/8., .75),
	new Point2D.Double(.25, 1/8.),
	new Point2D.Double(.25, 3/8.),
	new Point2D.Double(.25, 5/8.),
	new Point2D.Double(.25, 7/8.),
	new Point2D.Double(.75, 1/8.),
	new Point2D.Double(.75, 3/8.),
	new Point2D.Double(.75, 5/8.),
	new Point2D.Double(.75, 7/8.),
	new Line2D.Double(.25, 0., .25, 1.),
	new Line2D.Double(.75, 0., .75, 1.),
	new Line2D.Double(0., .25, 0., .25),
	new Line2D.Double(0., .75, 0., .75),
    };

    public Pgg() {
	super(NAMES, TRANSFORMS, PROPERTIES, SNAP_VORONOI);
    }

    public int getTileType() {
	return TILETYPE_RECTANGLE;
    }

}
