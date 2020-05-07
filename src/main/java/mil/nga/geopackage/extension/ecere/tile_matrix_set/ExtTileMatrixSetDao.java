package mil.nga.geopackage.extension.ecere.tile_matrix_set;

import com.j256.ormlite.support.ConnectionSource;
import mil.nga.geopackage.db.GeoPackageDao;

import java.sql.SQLException;

/**
 * @author jyutzler
 * @since 4.0.0
 */
public class ExtTileMatrixSetDao extends GeoPackageDao<ExtTileMatrixSet, Long> {

    /**
     * Constructor, required by ORMLite
     *
     * @param connectionSource
     *            connection source
     * @param dataClass
     *            data class
     * @throws SQLException
     *             upon failure
     */
    public ExtTileMatrixSetDao(ConnectionSource connectionSource,
                               Class<ExtTileMatrixSet> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }
}

