/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.leeds.ccg.andyt.projects.saric.process;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import java.io.File;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.TreeSetFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import uk.ac.leeds.ccg.andyt.agdtgeotools.AGDT_Geotools;
import uk.ac.leeds.ccg.andyt.agdtgeotools.AGDT_Shapefile;
import uk.ac.leeds.ccg.andyt.projects.saric.core.SARIC_Environment;
import uk.ac.leeds.ccg.andyt.projects.saric.core.SARIC_Object;
import uk.ac.leeds.ccg.andyt.projects.saric.core.SARIC_Strings;
import uk.ac.leeds.ccg.andyt.projects.saric.data.catchment.SARIC_Teifi;
import uk.ac.leeds.ccg.andyt.projects.saric.data.catchment.SARIC_Wissey;
import uk.ac.leeds.ccg.andyt.projects.saric.data.metoffice.datapoint.site.SARIC_Site;
import uk.ac.leeds.ccg.andyt.projects.saric.data.metoffice.datapoint.site.SARIC_SiteHandler;
import uk.ac.leeds.ccg.andyt.projects.saric.io.SARIC_Files;
import uk.ac.leeds.ccg.andyt.vector.core.Vector_Environment;
import uk.ac.leeds.ccg.andyt.vector.geometry.Vector_Envelope2D;
import uk.ac.leeds.ccg.andyt.vector.geometry.Vector_Point2D;
import uk.ac.leeds.ccg.andyt.vector.projection.Vector_OSGBtoLatLon;

/**
 *
 * @author geoagdt
 */
public class SARIC_CreatePointShapefiles extends SARIC_Object implements Runnable {

    // For convenience
    SARIC_Files sf;
    SARIC_Strings ss;
    Vector_Environment ve;

    // For processing
    boolean doForecasts;
    boolean doObservations;

    protected SARIC_CreatePointShapefiles() {
    }

    /**
     *
     * @param se
     * @param doForecasts If true then shapefile created for sites where there
     * are Forecasts.
     * @param doObservations If true then shapefile created for sites where
     * there are Observations.
     */
    public SARIC_CreatePointShapefiles(
            SARIC_Environment se,
            boolean doForecasts,
            boolean doObservations) {
        super(se);
        sf = se.getFiles();
        ss = se.getStrings();
        ve = se.getVector_Environment();
        this.doForecasts = doForecasts;
        this.doObservations = doObservations;
    }

    public static void main(String[] args) {
        new SARIC_CreatePointShapefiles().run();
    }

    public void run() {
        SARIC_SiteHandler sh;
        sh = new SARIC_SiteHandler(se);
        HashSet<SARIC_Site> sites;
        BigDecimal buffer;
        if (doForecasts) {
            String time;
            //time = ss.getString_daily();
            time = ss.getString_3hourly();
            buffer = null;
            sites = sh.getForecastsSites(time);
            run(ss.getString_Forecasts(), sites, buffer);
        }
        if (doObservations) {
//            buffer = new BigDecimal(20000.0d);
//            buffer = new BigDecimal(30000.0d);
//            buffer = new BigDecimal(40000.0d);
            buffer = new BigDecimal(60000.0d);
            sites = sh.getObservationsSites();
            run(ss.getString_Observations(), sites, buffer);
        }
    }

    public void run(String type, HashSet<SARIC_Site> sites, BigDecimal buffer) {
        // Initialise for Wissey
        SARIC_Wissey wissey;
        wissey = new SARIC_Wissey(se);
        Vector_Envelope2D wisseyBounds;
        wisseyBounds = wissey.getBoundsBuffered(buffer);
        
        // Initialise for Teifi
        SARIC_Teifi teifi;
        teifi = new SARIC_Teifi(se);
        Vector_Envelope2D teifiBounds;
        teifiBounds = teifi.getBoundsBuffered(buffer);
        
        SimpleFeatureType aPointSFT = null;
        try {
            aPointSFT = DataUtilities.createType(
                    "POINT",
                    "the_geom:Point:srid=27700,"
                    + "name:String,"
                    + "ID:Integer,"
                    + "Latitude:Double,"
                    + "Longitude:Double,"
                    + "Elevation:Double");
            //srid=27700 is the Great_Britain_National_Grid
        } catch (SchemaException ex) {
            Logger.getLogger(SARIC_CreatePointShapefiles.class.getName()).log(Level.SEVERE, null, ex);
        }

        TreeSetFeatureCollection tsfc;
        TreeSetFeatureCollection tsfcWissey;
        TreeSetFeatureCollection tsfcTeifi;
        tsfc = new TreeSetFeatureCollection();
        tsfcWissey = new TreeSetFeatureCollection();
        tsfcTeifi = new TreeSetFeatureCollection();
        SimpleFeatureBuilder sfb;
        sfb = new SimpleFeatureBuilder(aPointSFT);

        // Create SimpleFeatureBuilder
        //FeatureFactory ff = FactoryFinder.getGeometryFactories();
        GeometryFactory gF;
        gF = new GeometryFactory();
        String name;
        Coordinate c;
        Point point;
        SimpleFeature feature;

        Iterator<SARIC_Site> ite;
        ite = sites.iterator();
        SARIC_Site site;
        double[] OSGBEastingAndNorthing;
        Vector_Point2D p;
        while (ite.hasNext()) {
            site = ite.next();
            name = site.getName();
            OSGBEastingAndNorthing = Vector_OSGBtoLatLon.latlon2osgb(
                    site.getLatitude(), site.getLongitude());
            p = new Vector_Point2D(ve, OSGBEastingAndNorthing[0], OSGBEastingAndNorthing[1]);
            c = new Coordinate(p._x.doubleValue(), p._y.doubleValue());
            point = gF.createPoint(c);
            sfb.add(point);
            sfb.add(name);
            sfb.add(site.getId());
            sfb.add(site.getLatitude());
            sfb.add(site.getLongitude());
            sfb.add(site.getElevation());
            feature = sfb.buildFeature(name);
            tsfc.add(feature);
            if (wisseyBounds.getIntersects(p)) {
                tsfcWissey.add(feature);
            }
            if (teifiBounds.getIntersects(p)) {
                tsfcTeifi.add(feature);
            }
        }

        // Write out sites in the Wissey/Teifi
        File dir;
        dir = new File(
                sf.getGeneratedDataMetOfficeDataPointDir(),
                type);
        File outfile;
        outfile = AGDT_Geotools.getOutputShapefile(
                dir,
                "Sites");
        outfile.getParentFile().mkdirs();
        AGDT_Shapefile.transact(
                outfile,
                aPointSFT,
                tsfc,
                new ShapefileDataStoreFactory());
        outfile = AGDT_Geotools.getOutputShapefile(
                dir,
                ss.getString_Wissey() + "SitesBuffered");
        outfile.getParentFile().mkdirs();
        AGDT_Shapefile.transact(
                outfile,
                aPointSFT,
                tsfcWissey,
                new ShapefileDataStoreFactory());
        outfile = AGDT_Geotools.getOutputShapefile(
                dir,
                ss.getString_Teifi() + "SitesBuffered");
        outfile.getParentFile().mkdirs();
        AGDT_Shapefile.transact(
                outfile,
                aPointSFT,
                tsfcTeifi,
                new ShapefileDataStoreFactory());
    }

}
