import geopandas as gpd
import pandas as pd

def get_shapefile_size(file_in):
    shp_file = gpd.read_file(file_in)
    print(len(shp_file))

# get_shapefile_size('./curves.geojson')

def convert_shape_geojson(file_in, file_out):
    shp_file = gpd.read_file(file_in)
    shp_file.to_file(file_out, driver='GeoJSON')

# convert_shape_geojson('./crs 2240/All_District_Curve_Inventory.shp', './curves.geojson')

def convert_crs(file_in, file_out, crs):
    shp_file = gpd.read_file(file_in)
    data = shp_file.to_crs(epsg=crs)
    data.to_file(file_out)

# convert_crs('./shapefile/All_District_Curve_Inventory.shp', './crs 2240/All_District_Curve_Inventory.shp', 2240)

def generate_ground_truth(shapefile, gps_points):
    curve_data = gpd.read_file(shapefile)
    curve_data = curve_data.to_crs(epsg=2240)
    curve_data['geometry'] = curve_data.geometry.buffer(100, cap_style=2)

    df = pd.read_csv(gps_points)

    gps_df = gpd.GeoDataFrame(
        geometry=gpd.points_from_xy(df['longitude_dd'], df['latitude_dd'], crs="EPSG:4326"), data=df
    )

    gps_df = gps_df.to_crs(epsg=2240)
    join = gps_df.sjoin(curve_data, how="left", predicate='within')
    print(join.columns)
    pd.DataFrame(join.assign(geometry=join['geometry'].apply(lambda p: p.wkt))).to_csv('./georgia.csv')

#Function to clear up duplicates and ensure both the ground truth and java geospatial data is clean.
def equalize_data(python, java):
    curve_python = pd.read_csv(python)
    curve_java = pd.read_csv(java)
    curve_java = curve_java.drop_duplicates(subset=['lat', 'lon'], keep='first')
    curve_python = curve_python.drop_duplicates(subset=['latitude_dd', 'longitude_dd'], keep='first')
    curve_java = curve_java.drop(columns=['time(ms)', 'lat', 'lon']).reset_index()
    curve_python = curve_python.rename(columns={'latitude_dd': 'lat', 'longitude_dd': 'lon'})
    # curve_python.drop(curve_python.columns.difference(['lat', 'lon', 'c_segid', 'c_sectid', 'c_id']), 1, inplace=True)
    curve_python = curve_python[['c_segid', 'c_sectid', 'c_id']].reset_index()
    curve_java = curve_java.fillna(-1)
    curve_python = curve_python.fillna(-1)
    curve_java.to_csv('java.csv')
    curve_python.to_csv('python.csv')
    return curve_java, curve_python

#Function that counts the number of same curves ids between ground truth and java geospatial code.
def compare_data(python, java):
    count = 0
    for ind in java.index:
        if java['c_segid'][ind] == python['c_segid'][ind] and java['c_sectid'][ind] == python['c_sectid'][ind] and java['c_id'][ind] == python['c_id'][ind]:
            count += 1
    print(count)
    

#First file passed in is the ground truth, 2nd file is the results of the java application
curve_java, curve_python = equalize_data('georgia.csv', 'no_debug.csv')
compare_data(curve_java, curve_python)

def generate_capped_curves(shapefile, file_out):
    curve_data = gpd.read_file(shapefile)
    curve_data = curve_data.to_crs(epsg=2240)
    curve_data['geometry'] = curve_data.geometry.buffer(100, cap_style=2)

    curve_data.to_file(file_out, driver='GeoJSON')



def get_stats(csv):
    p = pd.read_csv(csv)
    print(f'Mean: {p["time(ms)"].mean()}')
    print(f'Median: {p["time(ms)"].median()}')
    print(f'P99: {p["time(ms)"].quantile(0.99)}')
    print(f'P95: {p["time(ms)"].quantile(0.95)}')
    print(f'Max: {p["time(ms)"].max()}')
    print(f'Min: {p["time(ms)"].min()}')

get_stats('debug.csv')




    


# generate_ground_truth('./crs 2240/All_District_Curve_Inventory.shp', '2023_10_05_17_14_07_252_loc.csv')
# generate_capped_curves('./crs 2240/All_District_Curve_Inventory.shp', './capped_georgia.geojson')
