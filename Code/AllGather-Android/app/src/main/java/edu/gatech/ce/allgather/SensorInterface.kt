package edu.gatech.ce.allgather

interface GPSSensor {
    fun getGPSData(): List<GPSData>
}

interface IMUSensor {
    fun getIMUData(): List<IMUData>
}
