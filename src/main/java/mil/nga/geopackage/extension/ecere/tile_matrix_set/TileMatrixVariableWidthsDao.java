package mil.nga.geopackage.extension.ecere.tile_matrix_set;

import com.j256.ormlite.support.ConnectionSource;
import mil.nga.geopackage.db.GeoPackageDao;

import java.sql.SQLException;

/**
 * @author jyutzler
 * @since 4.0.0
 */
public class TileMatrixVariableWidthsDao extends GeoPackageDao<TileMatrixVariableWidths, Long> {

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
    public TileMatrixVariableWidthsDao(ConnectionSource connectionSource,
                                       Class<TileMatrixVariableWidths> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }
}

