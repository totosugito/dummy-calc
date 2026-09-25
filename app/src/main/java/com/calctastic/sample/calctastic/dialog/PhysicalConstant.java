package com.calctastic.sample.calctastic.dialog;

/** Physical constants — port from com.calctastic.calculator.constants.PhysicalConstant */
public enum PhysicalConstant {
    ALPHA_PARTICLE_MASS("Alpha Particle Mass", "6.6446573450E-27", "mα", "kg"),
    ATOMIC_MASS_UNIT("Atomic Mass Unit", "1.66053906892E-27", "u", "kg"),
    AVOGADROS_NUMBER("Avogadro's Number", "6.02214076E23", "NA", "mol⁻¹"),
    BOHR_MAGNETON("Bohr Magneton", "9.2740100657E-24", "µB", "J·T⁻¹"),
    BOHR_RADIUS("Bohr Radius", "5.29177210544E-11", "a0", "m"),
    BOLTZMANN_CONSTANT("Boltzmann Constant", "1.380649E-23", "k", "J·K⁻¹"),
    CHAR_IMPEDANCE_OF_VACUUM("Char. Impedance of Vacuum", "376.730313412", "Z0", "Ω"),
    CLASSICAL_ELECTRON_RADIUS("Classical Electron Radius", "2.8179403205E-15", "re", "m"),
    COMPTON_WAVELENGTH("Compton Wavelength", "2.42631023538E-12", "λC", "m"),
    CONDUCTANCE_QUANTUM("Conductance Quantum", "7.748091729E-5", "G0", "S"),
    DEUTRON_MASS("Deutron Mass", "3.3435837768E-27", "md", "kg"),
    ELECTRIC_CONSTANT("Electric Constant", "8.8541878188E-12", "Є0", "F·m⁻¹"),
    ELECTRON_MASS("Electron Mass", "9.1093837139E-31", "me", "kg"),
    ELECTRON_VOLT("Electron Volt", "1.602176634E-19", "eV", "J"),
    ELEMENTARY_CHARGE("Elementary Charge", "1.602176634E-19", "𝑒", "C"),
    FARADAYS_CONSTANT("Faraday's Constant", "96485.33212", "F", "C·mol⁻¹"),
    FINE_STRUCTURE_CONSTANT("Fine Structure Constant", "7.2973525643E-3", "α", ""),
    GRAVITATIONAL_CONSTANT("Gravitational Constant", "6.67430E-11", "G", "m³·kg⁻¹·s⁻²"),
    HARTREE_ENERGY("Hartree Energy", "4.3597447222060E-18", "Eh", "J"),
    JOSEPHSON_CONSTANT("Josephson Constant", "4.835978484E14", "KJ", "Hz·V⁻¹"),
    MAGNETIC_CONSTANT("Magnetic Constant", "1.25663706127E-6", "µ0", "N·A⁻²"),
    MAGNETIC_FLUX_QUANTUM("Magnetic Flux Quantum", "2.067833848E-15", "Φ0", "Wb"),
    MOLAR_GAS_CONSTANT("Molar Gas Constant", "8.314462618", "R", "J·mol⁻¹·K⁻¹"),
    MUON_MASS("Muon Mass", "1.883531627E-28", "mµ", "kg"),
    NEUTRON_MASS("Neutron Mass", "1.67492750056E-27", "mn", "kg"),
    NUCLEAR_MAGNETON("Nuclear Magneton", "5.0507837393E-27", "µN", "J·T⁻¹"),
    PLANCK_CONSTANT("Planck Constant", "6.62607015E-34", "h", "J·Hz⁻¹"),
    PLANCK_CONSTANT_REDUCED("Planck Constant Reduced", "1.054571817E-34", "ħ", "J·s"),
    PLANCK_LENGTH("Planck Length", "1.616255E-35", "lP", "m"),
    PLANCK_MASS("Planck Mass", "2.176434E-8", "mP", "kg"),
    PLANCK_TEMPERATURE("Planck Temperature", "1.416784E32", "TP", "K"),
    PLANCK_TIME("Planck Time", "5.391247E-44", "tP", "s"),
    PROTON_MASS("Proton Mass", "1.67262192595E-27", "mp", "kg"),
    RYDBERG_CONSTANT("Rydberg Constant", "10973731.568157", "R∞", "m⁻¹"),
    SPEED_OF_LIGHT("Speed of Light in Vacuum", "299792458.", "c", "m·s⁻¹"),
    STANDARD_ATMOSPHERE("Standard Atmosphere", "101325.", "atm", "Pa"),
    STANDARD_GRAVITY("Standard Gravity", "9.80665", "g0", "m·s⁻²"),
    STEFAN_BOLTZMANN_CONSTANT("Stefan-Boltzmann Constant", "5.670374419E-8", "σ", "W·m⁻²·K⁻⁴"),
    TAU_MASS("Tau Mass", "3.16754E-27", "mτ", "kg"),
    THOMSON_CROSS_SECTION("Thomson Cross Section", "6.6524587051E-29", "σe", "m²"),
    TRITON_MASS("Triton Mass", "5.0073567512E-27", "mt", "kg"),
    VON_KLITZING_CONSTANT("von Klitzing Constant", "25812.80745", "RK", "Ω"),
    WIEN_FREQUENCY_DISPLACEMENT("Wien Frequency Disp.", "58789257570.", "b'", "Hz·K⁻¹"),
    WIEN_WAVELENGTH_DISPLACEMENT("Wien Wavelength Disp.", "2.897771955E-3", "b", "m·K");

    private final String description;
    private final String value;
    private final String symbol;
    private final String units;

    PhysicalConstant(String description, String value, String symbol, String units) {
        this.description = description;
        this.value = value;
        this.symbol = symbol;
        this.units = units;
    }

    public String getDescription() { return description; }
    public String getValue() { return value; }
    public String getSymbol() { return symbol; }
    public String getUnits() { return units; }
}
