package mil.nga.geopackage.tiles.reproject;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackageCore;
import mil.nga.geopackage.GeoPackageException;
import mil.nga.geopackage.contents.Contents;
import mil.nga.geopackage.db.master.SQLiteMaster;
import mil.nga.geopackage.db.master.SQLiteMasterColumn;
import mil.nga.geopackage.db.master.SQLiteMasterQuery;
import mil.nga.geopackage.io.GeoPackageProgress;
import mil.nga.geopackage.srs.SpatialReferenceSystem;
import mil.nga.geopackage.tiles.TileGrid;
import mil.nga.geopackage.tiles.matrix.TileMatrix;
import mil.nga.geopackage.tiles.matrixset.TileMatrixSet;
import mil.nga.geopackage.tiles.user.TileColumn;
import mil.nga.geopackage.tiles.user.TileTable;
import mil.nga.geopackage.tiles.user.TileTableMetadata;
import mil.nga.geopackage.user.UserCoreDao;
import mil.nga.sf.proj.Projection;
import mil.nga.sf.proj.ProjectionTransform;

/**
 * Tile Reprojection for reprojecting an existing tile table
 * 
 * @author osbornb
 * @since 4.0.1
 */
public abstract class TileReprojectionCore {

	/**
	 * Optional optimization
	 */
	protected TileReprojectionOptimize optimize;

	/**
	 * Overwrite existing tiles at a zoom level when geographic calculations
	 * differ
	 */
	protected boolean overwrite = false;

	/**
	 * Tile width in pixels
	 */
	protected Long tileWidth;

	/**
	 * Tile height in pixels
	 */
	protected Long tileHeight;

	/**
	 * Progress callbacks
	 */
	protected GeoPackageProgress progress;

	/**
	 * Tile DAO
	 */
	protected UserCoreDao<TileColumn, TileTable, ?, ?> tileDao;

	/**
	 * GeoPackage
	 */
	protected GeoPackageCore geoPackage;

	/**
	 * Table name
	 */
	protected String table;

	/**
	 * Projection
	 */
	protected Projection projection;

	/**
	 * Tile DAO
	 */
	protected UserCoreDao<TileColumn, TileTable, ?, ?> reprojectTileDao;

	/**
	 * Replace flag
	 */
	protected boolean replace = false;

	/**
	 * Zoom level configuration map
	 */
	protected Map<Long, TileReprojectionZoom> zoomConfigs = new HashMap<>();

	/**
	 * Optimization tile grid
	 */
	protected TileGrid optimizeTileGrid;

	/**
	 * Optimization zoom
	 */
	protected long optimizeZoom;

	/**
	 * Constructor
	 * 
	 * @param tileDao
	 *            tile DAO
	 * @param geoPackage
	 *            GeoPackage
	 * @param table
	 *            table name
	 * @param projection
	 *            projection
	 */
	protected TileReprojectionCore(
			UserCoreDao<TileColumn, TileTable, ?, ?> tileDao,
			GeoPackageCore geoPackage, String table, Projection projection) {
		this.tileDao = tileDao;
		this.geoPackage = geoPackage;
		this.table = table;
		this.projection = projection;
	}

	/**
	 * Constructor
	 * 
	 * @param tileDao
	 *            tile DAO
	 * @param reprojectTileDao
	 *            reprojection tile DAO
	 */
	protected TileReprojectionCore(
			UserCoreDao<TileColumn, TileTable, ?, ?> tileDao,
			UserCoreDao<TileColumn, TileTable, ?, ?> reprojectTileDao) {
		this.tileDao = tileDao;
		this.reprojectTileDao = reprojectTileDao;
	}

	/**
	 * Constructor
	 * 
	 * @param tileDao
	 *            tile DAO
	 * @param geoPackage
	 *            GeoPackage
	 * @param reprojectTileDao
	 *            reprojection tile DAO
	 */
	protected TileReprojectionCore(
			UserCoreDao<TileColumn, TileTable, ?, ?> tileDao,
			GeoPackageCore geoPackage,
			UserCoreDao<TileColumn, TileTable, ?, ?> reprojectTileDao) {
		this(tileDao, reprojectTileDao);
		this.geoPackage = geoPackage;
	}

	/**
	 * Get the optimization minimum zoom level
	 * 
	 * @return zoom level
	 */
	protected abstract long getOptimizeZoom();

	/**
	 * Create the reprojection tile DAO
	 * 
	 * @param table
	 *            table name
	 * @return reprojection tile DAO
	 */
	protected abstract UserCoreDao<TileColumn, TileTable, ?, ?> createReprojectTileDao(
			String table);

	/**
	 * Get the reprojection tile matrix set
	 * 
	 * @return tile matrix set
	 */
	protected abstract TileMatrixSet getTileMatrixSet();

	/**
	 * Get the reprojection tile matrices
	 * 
	 * @return tile matrix matrices
	 */
	protected abstract List<TileMatrix> getTileMatrices();

	/**
	 * Delete the table tile matrices
	 * 
	 * @param table
	 *            table name
	 */
	protected abstract void deleteTileMatrices(String table);

	/**
	 * Get the optimization
	 * 
	 * @return optimization
	 */
	public TileReprojectionOptimize getOptimize() {
		return optimize;
	}

	/**
	 * Set the optimization
	 * 
	 * @param optimize
	 *            optimization
	 */
	public void setOptimize(TileReprojectionOptimize optimize) {
		this.optimize = optimize;
	}

	/**
	 * Is overwrite enabled
	 * 
	 * @return overwrite flag
	 */
	public boolean isOverwrite() {
		return overwrite;
	}

	/**
	 * Set the overwrite flag
	 * 
	 * @param overwrite
	 *            overwrite flag
	 */
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	/**
	 * Get the tile width
	 * 
	 * @return tile width
	 */
	public Long getTileWidth() {
		return tileWidth;
	}

	/**
	 * Set the tile width
	 * 
	 * @param tileWidth
	 *            tile width
	 */
	public void setTileWidth(Long tileWidth) {
		this.tileWidth = tileWidth;
	}

	/**
	 * Get the tile height
	 * 
	 * @return tile height
	 */
	public Long getTileHeight() {
		return tileHeight;
	}

	/**
	 * Set the tile height
	 * 
	 * @param tileHeight
	 *            tile height
	 */
	public void setTileHeight(Long tileHeight) {
		this.tileHeight = tileHeight;
	}

	/**
	 * Get the progress callbacks
	 * 
	 * @return progress
	 */
	public GeoPackageProgress getProgress() {
		return progress;
	}

	/**
	 * Set the progress callbacks
	 * 
	 * @param progress
	 *            progress callbacks
	 */
	public void setProgress(GeoPackageProgress progress) {
		this.progress = progress;
	}

	/**
	 * Get the zoom level configurations
	 *
	 * @return zoom configs
	 */
	public Map<Long, TileReprojectionZoom> getZoomConfigs() {
		return zoomConfigs;
	}

	/**
	 * Get the zoom level configuration for a zoom level
	 *
	 * @param zoom
	 *            from zoom level
	 * @return zoom config
	 */
	public TileReprojectionZoom getConfig(long zoom) {
		return zoomConfigs.get(zoom);
	}

	/**
	 * Get the zoom level configuration or create new configuration for a zoom
	 * level
	 *
	 * @param zoom
	 *            from zoom level
	 * @return zoom config
	 */
	public TileReprojectionZoom getConfigOrCreate(long zoom) {
		TileReprojectionZoom config = getConfig(zoom);
		if (config == null) {
			config = new TileReprojectionZoom(zoom);
			setConfig(config);
		}
		return config;
	}

	/**
	 * Set a zoom level configuration for a zoom level
	 *
	 * @param config
	 *            zoom configuration
	 */
	public void setConfig(TileReprojectionZoom config) {
		zoomConfigs.put(config.getZoom(), config);
	}

	/**
	 * Set a reprojected to zoom level for a zoom level
	 * 
	 * @param zoom
	 *            zoom level
	 * @param toZoom
	 *            reprojected zoom level
	 */
	public void setToZoom(long zoom, long toZoom) {
		getConfigOrCreate(zoom).setToZoom(toZoom);
	}

	/**
	 * Get a reprojected to zoom level from a zoom level, defaults as the zoom
	 * level if not set
	 *
	 * @param zoom
	 *            zoom level
	 * @return reprojected to zoom level
	 */
	public long getToZoom(long zoom) {
		long toZoom = zoom;
		TileReprojectionZoom config = getConfig(zoom);
		if (config != null && config.hasToZoom()) {
			toZoom = config.getToZoom();
		}
		return toZoom;
	}

	/**
	 * Set a reprojected tile width for a zoom level
	 *
	 * @param tileWidth
	 *            reprojected tile width
	 * @param zoom
	 *            zoom level
	 */
	public void setTileWidth(long zoom, long tileWidth) {
		getConfigOrCreate(zoom).setTileWidth(tileWidth);
	}

	/**
	 * Get a reprojected tile width from a zoom level
	 *
	 * @param zoom
	 *            zoom level
	 * @return reprojected tile width
	 */
	public Long getTileWidth(long zoom) {
		Long tileWidth = this.tileWidth;
		TileReprojectionZoom config = getConfig(zoom);
		if (config != null && config.hasTileWidth()) {
			tileWidth = config.getTileWidth();
		}
		return tileWidth;
	}

	/**
	 * Set a reprojected tile height for a zoom level
	 * 
	 * @param zoom
	 *            zoom level
	 * @param tileHeight
	 *            reprojected tile height
	 */
	public void setTileHeight(long zoom, long tileHeight) {
		getConfigOrCreate(zoom).setTileHeight(tileHeight);
	}

	/**
	 * Get a reprojected tile height from a zoom level
	 *
	 * @param zoom
	 *            zoom level
	 * @return reprojected tile height
	 */
	public Long getTileHeight(long zoom) {
		Long tileHeight = this.tileHeight;
		TileReprojectionZoom config = getConfig(zoom);
		if (config != null && config.hasTileHeight()) {
			tileHeight = config.getTileHeight();
		}
		return tileHeight;
	}

	/**
	 * Set a reprojected matrix width for a zoom level
	 *
	 * @param matrixWidth
	 *            reprojected matrix width
	 * @param zoom
	 *            zoom level
	 */
	public void setMatrixWidth(long zoom, long matrixWidth) {
		getConfigOrCreate(zoom).setMatrixWidth(matrixWidth);
	}

	/**
	 * Get a reprojected matrix width from a zoom level
	 *
	 * @param zoom
	 *            zoom level
	 * @return reprojected matrix width
	 */
	public Long getMatrixWidth(long zoom) {
		Long matrixWidth = null;
		TileReprojectionZoom config = getConfig(zoom);
		if (config != null && config.hasMatrixWidth()) {
			matrixWidth = config.getMatrixWidth();
		}
		return matrixWidth;
	}

	/**
	 * Set a reprojected matrix height for a zoom level
	 *
	 * @param matrixHeight
	 *            reprojected matrix height
	 * @param zoom
	 *            zoom level
	 */
	public void setMatrixHeight(long zoom, long matrixHeight) {
		getConfigOrCreate(zoom).setMatrixHeight(matrixHeight);
	}

	/**
	 * Get a reprojected matrix height from a zoom level
	 *
	 * @param zoom
	 *            zoom level
	 * @return reprojected matrix height
	 */
	public Long getMatrixHeight(long zoom) {
		Long matrixHeight = null;
		TileReprojectionZoom config = getConfig(zoom);
		if (config != null && config.hasMatrixHeight()) {
			matrixHeight = config.getMatrixHeight();
		}
		return matrixHeight;
	}

	/**
	 * Initialize the reprojection
	 */
	protected void initialize() {

		if (reprojectTileDao == null) {

			BoundingBox boundingBox = tileDao.getBoundingBox(projection);
			BoundingBox contentsBoundingBox = boundingBox;
			SpatialReferenceSystem srs;
			try {
				srs = geoPackage.getSpatialReferenceSystemDao()
						.getOrCreate(projection);
			} catch (SQLException e) {
				throw new GeoPackageException(
						"Failed to create Spatial Reference System for projection. Authority: "
								+ projection.getAuthority() + ", Code: "
								+ projection.getCode());
			}

			if (tileDao.getDatabase().equals(geoPackage.getName())
					&& tileDao.getTableName().equalsIgnoreCase(table)) {
				// Replacing source table, find a temp table name for the
				// reprojections
				int count = 1;
				String tempTable = table + "_" + (++count);
				while (SQLiteMaster.count(tileDao.getDb(), SQLiteMasterQuery
						.create(SQLiteMasterColumn.NAME, tempTable)) > 0) {
					tempTable = table + "_" + (++count);
				}
				table = tempTable;
				replace = true;
			}

			if (optimize != null) {
				boundingBox = optimize(boundingBox);
			}

			TileTable tileTable = null;
			if (geoPackage.isTable(table)) {

				if (!geoPackage.isTileTable(table)) {
					throw new GeoPackageException(
							"Table exists and is not a tile table: " + table);
				}

				reprojectTileDao = createReprojectTileDao(table);

				if (!reprojectTileDao.getProjection().equals(projection)) {
					throw new GeoPackageException(
							"Existing tile table projection differs from the reprojection. Table: "
									+ table + ", Projection: " + projection
									+ ", Reprojection: "
									+ reprojectTileDao.getProjection());
				}

				TileMatrixSet tileMatrixSet = getTileMatrixSet();
				List<TileMatrix> tileMatrices = getTileMatrices();

				if (tileMatrices.size() > 0) {

					TileMatrix tileMatrix = tileMatrices.get(0);

					if (Math.abs(tileMatrixSet.getMinX()
							- boundingBox.getMinLongitude()) > tileMatrix
									.getPixelXSize()
							|| Math.abs(tileMatrixSet.getMinY()
									- boundingBox.getMinLatitude()) > tileMatrix
											.getPixelYSize()
							|| Math.abs(tileMatrixSet.getMaxX() - boundingBox
									.getMaxLongitude()) > tileMatrix
											.getPixelXSize()
							|| Math.abs(tileMatrixSet.getMaxY()
									- boundingBox.getMaxLatitude()) > tileMatrix
											.getPixelYSize()) {

						if (!overwrite) {
							throw new GeoPackageException(
									"Existing Tile Matrix Set Geographic Properties differ. Enable 'overwrite' to replace all tiles. GeoPackage: "
											+ reprojectTileDao.getDatabase()
											+ ", Tile Table: "
											+ reprojectTileDao.getTableName());
						}

						deleteTileMatrices(table);
						reprojectTileDao.deleteAll();

					}

				}

				Contents contents = reprojectTileDao.getContents();
				contents.setSrs(srs);
				contents.setMinX(contentsBoundingBox.getMinLongitude());
				contents.setMinY(contentsBoundingBox.getMinLatitude());
				contents.setMaxX(contentsBoundingBox.getMaxLongitude());
				contents.setMaxY(contentsBoundingBox.getMaxLatitude());
				try {
					geoPackage.getContentsDao().update(contents);
				} catch (SQLException e) {
					throw new GeoPackageException(
							"Failed to update reprojection tile table contents. GeoPackage: "
									+ reprojectTileDao.getDatabase()
									+ ", Table: "
									+ reprojectTileDao.getTableName());
				}

				tileMatrixSet.setSrs(srs);
				tileMatrixSet.setMinX(boundingBox.getMinLongitude());
				tileMatrixSet.setMinY(boundingBox.getMinLatitude());
				tileMatrixSet.setMaxX(boundingBox.getMaxLongitude());
				tileMatrixSet.setMaxY(boundingBox.getMaxLatitude());
				try {
					geoPackage.getTileMatrixSetDao().update(tileMatrixSet);
				} catch (SQLException e) {
					throw new GeoPackageException(
							"Failed to update reprojection tile matrix set. GeoPackage: "
									+ reprojectTileDao.getDatabase()
									+ ", Table: "
									+ reprojectTileDao.getTableName());
				}

			} else {
				tileTable = geoPackage.createTileTable(
						TileTableMetadata.create(table, contentsBoundingBox,
								boundingBox, srs.getSrsId()));
				reprojectTileDao = createReprojectTileDao(
						tileTable.getTableName());
			}

		}

	}

	/**
	 * Finish the reprojection
	 */
	protected void finish() {
		boolean active = isActive();
		if (replace) {
			if (active) {
				geoPackage.deleteTable(tileDao.getTableName());
				geoPackage.copyTable(reprojectTileDao.getTableName(),
						tileDao.getTableName());
				geoPackage.deleteTable(reprojectTileDao.getTableName());
				reprojectTileDao = null;
			}
			table = tileDao.getTableName();
			replace = false;
		}
		if (progress != null && !active && progress.cleanupOnCancel()) {
			if (geoPackage == null) {
				throw new GeoPackageException(
						"Reprojeciton cleanup not supported when constructed without the GeoPackage. GeoPackage:"
								+ reprojectTileDao.getDatabase()
								+ ", Tile Table: "
								+ reprojectTileDao.getTableName());
			}
			geoPackage.deleteTable(reprojectTileDao.getTableName());
		}
	}

	/**
	 * Optimize the bounding box
	 * 
	 * @param boundingBox
	 *            bounding box
	 * @return optimized bounding box
	 */
	protected BoundingBox optimize(BoundingBox boundingBox) {

		if (optimize.isWorld()) {
			optimizeZoom = 0;
			optimizeTileGrid = optimize.tileGrid();
			boundingBox = optimize.boundingBox();
			ProjectionTransform transform = optimize.projection()
					.getTransformation(projection);
			if (!transform.isSameProjection()) {
				boundingBox = boundingBox.transform(transform);
			}
		} else {
			optimizeZoom = getOptimizeZoom();
			ProjectionTransform transform = projection
					.getTransformation(optimize.projection());
			if (!transform.isSameProjection()) {
				boundingBox = boundingBox.transform(transform);
			}
			optimizeTileGrid = optimize.tileGrid(boundingBox, optimizeZoom);
			boundingBox = optimize.boundingBox(optimizeTileGrid, optimizeZoom);
			if (!transform.isSameProjection()) {
				boundingBox = boundingBox
						.transform(transform.getInverseTransformation());
			}
		}

		return boundingBox;
	}

	/**
	 * Check if currently active
	 * 
	 * @return true if active
	 */
	protected boolean isActive() {
		return progress == null || progress.isActive();
	}

}
