/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
package resources.objects.quadtree;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestQuadTree {
	
	@Test
	public void testInsertGet() {
		QuadTree<Double> tree = new QuadTree<Double>(0, 0, 10, 10);
		tree.put(5, 5, 5.0);
		Assert.assertEquals(5, (double)tree.getIgnoreCollisions(5, 5), 1E-10);
		tree.put(0, 0, 10.0);
		Assert.assertEquals(10, (double)tree.getIgnoreCollisions(0, 0), 1E-10);
		tree.put(10, 10, 15.0);
		Assert.assertEquals(15, (double)tree.getIgnoreCollisions(10, 10), 1E-10);
		tree.put(0, 0, 5.0);
		tree.put(0, 0, 2.5);
		Assert.assertEquals(10, (double)tree.getIgnoreCollisions(0, 0), 1E-10);
		Double [] vals = tree.get(0, 0).toArray(new Double[3]);
		Assert.assertArrayEquals(new Double[]{10.0, 5.0, 2.5}, vals);
		Assert.assertEquals(3, tree.remove(0, 0));
		Assert.assertNull(tree.getIgnoreCollisions(0, 0));
		for (int i = 0; i < 1000; i++) {
			float x = (float) (Math.random() * 10);
			float y = (float) (Math.random() * 10);
			double v = Math.random() * 1000;
			tree.put(x, y, v);
			Assert.assertEquals(v, (double)tree.getIgnoreCollisions(x, y), 1E-10);
			tree.remove(x, y);
			Assert.assertNull(tree.getIgnoreCollisions(x, y));
		}
	}
	
	@Test
	public void testDuplicatesBug() {
		QuadTree<Double> tree = new QuadTree<Double>(0, 0, 10, 10);
		Double d = new Double(5.0);
		tree.put(5.0, 5.0, d);
		Assert.assertEquals(1, tree.getWithinRange(5.0, 5.0, 10).size());
		Assert.assertEquals(tree.getIgnoreCollisions(5.0, 5.0), d);
		tree.remove(5, 5);
		Assert.assertEquals(0, tree.getWithinRange(5.0, 5.0, 10).size());
		tree.put(7.5, 7.5, d);
		Assert.assertEquals(1, tree.getWithinRange(5.0, 5.0, 10).size());
		Assert.assertEquals(tree.getIgnoreCollisions(7.5, 7.5), d);
		tree.remove(7.5, 7.5);
		Assert.assertEquals(0, tree.getWithinRange(5.0, 5.0, 10).size());
		tree.put(0, 0, d);
		Assert.assertEquals(1, tree.getWithinRange(5.0, 5.0, 10).size());
		Assert.assertEquals(tree.getIgnoreCollisions(0, 0), d);
		tree.remove(0, 0);
		for (int i = 0; i < 100; i++) {
			double x = Math.random();
			double y = Math.random();
			tree.put(x, y, d);
			Assert.assertEquals(1, tree.getWithinRange(5.0, 5.0, 10).size());
			Assert.assertEquals(tree.getIgnoreCollisions(x, y), d);
			Assert.assertTrue(tree.remove(x, y, d));
			Assert.assertEquals(0, tree.getWithinRange(5.0, 5.0, 10).size());
		}
	}
	
	@Test
	public void testWithinArea() {
		List <Point2D> points = new ArrayList<Point2D>();
		QuadTree <Point2D> tree = new QuadTree<Point2D>(0, 0, 10, 10);
		for (double x = 0; x < 10; x += 0.15) {
			for (double y = 0; y < 10; y += 0.15) {
				x = ((int)(x*100))/100.0;
				y = ((int)(y*100))/100.0;
				Point2D p = new Point2D(x, y);
				points.add(p);
				tree.put(x, y, p);
			}
		}
		for (double x = 0; x < 1000; x += 25) {
			for (double y = 0; y < 1000; y += 25) {
				x /= 100;
				y /= 100;
				test(points, tree, x, y, .5);
				x *= 100;
				y *= 100;
			}
		}
	}
	
	@Test
	public void testIntersectRandom() {
		double minX = 4;
		double minY = 4;
		double maxX = 7.5;
		double maxY = 7.5;
		double range = 2.5;
		for (int i = 0; i < 5000; i++) {
			double x = Math.random() * 10;
			double y = Math.random() * 10;
			if (x < minX || y < minY || x > maxX || y > maxY)
				continue;
			boolean inter = intersects(minX, minY, maxX, maxY, x, y, range);
			boolean actual = (x>=(minX-range)&&x<=(maxX+range)) && (y>=(minY-range)&&y<=(maxY+range));
			Assert.assertEquals("Failed at ("+x+", "+y+")", actual, inter);
			inter = intersects(minX, minY, maxX, maxY, x, y, range);
			Assert.assertEquals("Failed at ("+x+", "+y+")", actual, inter);
		}
		long start = System.nanoTime();
		for (int i = 0; i < 1000000; i++) {
			double x = Math.random() * 10;
			double y = Math.random() * 10;
			intersects(minX, minY, maxX, maxY, x, y, range);
		}
		long end = System.nanoTime();
		System.out.printf("Time per intersect: %.10fms%n", (end-start)/1E6/1000000);
	}
	
	@Test
	public void testIntersects() {
		double minX = 4;
		double minY = 4;
		double maxX = 7.5;
		double maxY = 7.5;
		double range = 2.5;
		for (double y = 0; y < 10; y += 0.1) {
			y = ((int)(y*100))/100.0;
			boolean inter = intersects(minX, minY, maxX, maxY, 0, y, range);
			Assert.assertEquals("Failed at (0, "+y+")", Math.sqrt(minX*minX+Math.pow(y-minY,2))<=range, inter);
			inter = intersects(minX, minY, maxX, maxY, 10, y, range);
			double nearest = (y < minY) ? minY : (y > maxY) ? maxY : y;
			Assert.assertEquals("Failed at (10, "+y+")", Math.sqrt((10-maxX)*(10-maxX)+Math.pow(y-nearest,2))<=range, inter);
		}
		for (double x = 0; x < 10; x += 0.1) {
			x = ((int)(x*100))/100.0;
			boolean inter = intersects(minX, minY, maxX, maxY, x, 0, range);
			Assert.assertEquals("Failed at ("+x+", 0)", Math.sqrt(minY*minY+Math.pow(x-minX,2))<=range, inter);
			inter = intersects(minX, minY, maxX, maxY, x, 10, range);
			double nearest = (x < minX) ? minX : (x > maxX) ? maxX : x;
			Assert.assertEquals("Failed at ("+x+", 10)", Math.sqrt((10-maxY)*(10-maxY)+Math.pow(x-nearest,2))<=range, inter);
		}
	}
	
	private boolean intersects(double minX, double minY, double maxX, double maxY, double x, double y, double range) {
		if (x < minX) {
			if (y < minY)
				return distance(x, y, minX, minY) <= range;
			else if (y > maxY)
				return distance(x, y, minX, maxY) <= range;
			return x >= minX-range;
		} else if (x > maxX) {
			if (y < minY)
				return distance(x, y, maxX, minY) <= range;
			else if (y > maxY)
				return distance(x, y, maxX, maxY) <= range;
			return x <= maxX+range;
		}
		return y >= minY-range && y <= maxY+range;
	}
	
	private double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
	}
	
	private void test(List <Point2D> points, QuadTree <Point2D> tree, double x, double y, double range) {
		int expected = getExpectedWithinRange(points, x, y, range);
//		System.out.println("-----------");
		int actual = tree.getWithinRange(x, y, range).size();
//		System.out.println("-----------");
//		if (expected != actual) {
//			List <Point2D> actualPoints = tree.getWithinRange(x, y, range);
//			List <Point2D> pCopy = new ArrayList<Point2D>();
//			for (Point2D p : points) {
//				if (Math.sqrt(Math.pow(p.x-x, 2) + Math.pow(p.y-y, 2)) <= range) {
//					pCopy.add(p);
//				}
//			}
//			System.out.println(expected + ", " + pCopy.size() + ", " + actualPoints.size());
//			pCopy.removeAll(actualPoints);
//			System.out.println(pCopy.size());
//			System.out.println(Arrays.toString(pCopy.toArray()));
//			for (Point2D p : pCopy)
//				System.out.println(p + " Dist: " + Math.sqrt(Math.pow(p.x-x,2)+Math.pow(p.y-y,2)));
//		}
		Assert.assertEquals("Failed at (" + x + ", " + y + ")", expected, actual);
	}
	
	private int getExpectedWithinRange(List <Point2D> points, double x, double y, double range) {
		int count = 0;
		for (Point2D p : points) {
			if (Math.sqrt(Math.pow(p.x-x, 2) + Math.pow(p.y-y, 2)) <= range) {
//				System.out.printf("Matched at (%.2f, %.2f) Dist %.3f%n", p.x, p.y, Math.sqrt(Math.pow(p.x-x,2) + Math.pow(p.y-y,2)));
				count++;
			}
		}
		return count;
	}
	
	private class Point2D {
		private double x;
		private double y;
		public Point2D(double x, double y) {
			this.x = x;
			this.y = y;
		}
		public String toString() {
			return "(" + x + ", " + y + ")";
		}
	}
	
}
