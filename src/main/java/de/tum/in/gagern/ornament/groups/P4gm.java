/* $Id: P4gm.java,v 1.5 2003/02/16 23:43:15 gagern Exp $

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
 * The crystallographic group p4gm.
 *
 * @author Martin von Gagern
 * @version $Revision: 1.5 $
 */
public class P4gm extends Group implements Constants {

    static final String[] NAMES={"p4g", "4*2"};

    static final double[][] TRANSFORMS={
	{1, 0, 0, 1, 0, 0}, {0, 1, 1, 0, 0, 0},
	{-1, 0, 0, 1, .5, .5}, {0, 1, -1, 0, .5, .5},
	{1, 0, 0, -1, .5, .5}, {0, -1, 1, 0, .5, .5},
	{-1, 0, 0, -1, 1, 1}, {0, -1, -1, 0, 1, 1}
    };

    static final Property[] PROPERTIES={
	new Property.Turn2(0, 0),
	new Property.Turn2(.5, .5),
	new Property.Turn4(0, .5),
	new Property.Turn4(.5, 0),
	new Property.Mirror(0, 0, 1, 1),
	new Property.Mirror(1, 0, 0, 1),
	new Property.Glide(0, .25, 1, .25, 1),
	new Property.Glide(0, .75, 1, .75, 1),
	new Property.Glide(.25, 0, .25, 1, 1),
	new Property.Glide(.75, 0, .75, 1, 1),
	new Property.Glide(.5, 0, 0, .5, 2),
	new Property.Glide(.5, 0, 1, .5, 2),
	new Property.Glide(.5, 1, 0, .5, 2),
	new Property.Glide(.5, 1, 1, .5, 2)
    };

    static final Object[] SNAP_VORONOI={
	new Point2D.Double(.25, 0.),
	new Point2D.Double(.75, 0.),
	new Point2D.Double(0., .25),
	new Point2D.Double(.5, .25),
	new Point2D.Double(1., .25),
	new Point2D.Double(.25, .5),
	new Point2D.Double(.75, .5),
	new Point2D.Double(0., .75),
	new Point2D.Double(.5, .75),
	new Point2D.Double(1., .75),
	new Point2D.Double(.25, 1.),
	new Point2D.Double(.75, 1.),
	new Line2D.Double(.5, 0., 1., .5),
	new Line2D.Double(1., .5, .5, 1.),
	new Line2D.Double(.5, 1., 0., .5),
	new Line2D.Double(0., .5, .5, 0.),
	new Line2D.Double(0., .5, 1., .5),
	new Line2D.Double(.5, 0., .5, 1.),
    };

    public P4gm() {
	super(NAMES, TRANSFORMS, PROPERTIES, SNAP_VORONOI);
    }

    public int getTileType() {
	return TILETYPE_SQUARE;
    }

}
