'use strict';
var app = angular.module('angularApp')
	.controller('SetupCharacterizationCtrl', function ($scope,$http,$modal,sampleService,$location,$anchorScroll,$filter,$upload,$timeout) {

    // define variables //
    $scope.sampleData = sampleService.sampleData;
    $scope.sampleId = sampleService.sampleId.data;
    $scope.domainFileUri = "";
    $scope.data = {};
    $scope.sampleMessage = sampleService.message.data;
    $scope.operands = ['='];
    $scope.PE = {};
    // can remove this after done testing local data
    $scope.dataCopy = angular.copy($scope.data);
    $scope.type = sampleService.type.data;
    $scope.isEdit = sampleService.isEdit.data;
    $scope.charId = sampleService.charId.data;
    $scope.charClassName = sampleService.charClassName.data;
    $scope.techniqueInstrument = {};
    $scope.loader = true;
    $scope.loaderMessage = "Loading";

    /* File Variables */
    $scope.usingFlash = FileAPI && FileAPI.upload != null;
    $scope.fileReaderSupported = window.FileReader != null && (window.FileAPI == null || FileAPI.html5 != false);
    $scope.fileForm = {};
    $scope.fileForm.uriExternal = false;
    $scope.externalUrlEnabled = false;
    $scope.addNewFile = false;
    $scope.selectedFileName = '';

    /* csv upload */
    var csvColumnMaxCount = 25;  // Maximum number of columns allowed
    var csvMaxNumberOfLines = 5000;  // Maximum number of rows allowed
    var csvMaxLenOfEntry = 200;
    var runaway = 10240; // A counter used to prevent an endless loop if something goes wrong.  @TODO needs a better name
    var csvDataColCount = 0;
    var csvDataObj;
    var csvDataRowCount;
    var csvImportError = '';

    $scope.badFindingCell = [];
        var resetColumnConfirmDialog;

    $scope.disableChangeColumnOrder = false;

    var uploadUrl = '/caNanoLab/rest/core/uploadFile';
    $scope.ie9 = false;
    if(navigator.appVersion.indexOf("MSIE 9.")!=-1){
        uploadUrl = '/caNanoLab/uploadFile';
        $scope.ie9 = true;
    }

    //$scope.data =  {"type":"physico-chemical characterization","name":"physical state","parentSampleId":67698688,"charId":100237312,"assayType":"physical state","protocolId":0,"characterizationSourceId":0,"characterizationDate":null,"charNamesForCurrentType":["molecular weight","other_pc","physical state","purity","relaxivity","shape","size","solubility","surface","zeta potential","other"],"property":{"@type":"SimplePhysicalState","propertyName":"PhysicalState","propertyDisplayName":"PhysicalState","type":"","typeOptions":["3D-cube","3D-cylinder","colloid-emulsion","colloid-gel","colloid-sol","fluid-gas","fluid-liquid","fluid-vapor","solid-crystal","solid-fibril","solid-glass","solid-granule","solid-powder","other"],"propertyViewTitles":["Type"],"propertyViewValues":[""]},"designMethodsDescription":null,"techniqueInstruments":{"experiments":[{"id":102760448,"displayName":"atomic force microscopy","techniqueType":"atomic force microscopy","abbreviation":null,"description":"","dirty":false,"instruments":[],"parentCharType":"","parentCharName":""}],"techniqueTypeLookup":["acoustic microscopy","asymmetrical flow field-flow fractionation with multi-angle laser light scattering","atomic absorption spectroscopy","atomic force microscopy","biochemical quantitation","capillary electrophoresis","cell counting","centrifugal filtration","coagulation detection","colony counting","confocal laser scanning microscopy","coulter principle","dark field microscopy","deconvolution fluorescence microscopy","differential centrifugal sedimentation","dynamic light scattering","electron microprobe analysis","electron spin resonance","electron spin resonance spectroscopy","electrophoretic light scattering","elemental analysis","energy dispersive spectroscopy","environmental transmission electron microscopy","fast protein liquid chromatography","flow cytometry","fluorescence microscopy","fluorometry","focused ion beam - scanning electron microscopy","fourier transform infrared spectrophotometry","gas chromatography","gas sorption","gel electrophoresis","gel filtration chromatography","gel permeation chromatography","high performance liquid chromatography","high performance liquid chromatography - evaporative light scattering detection","high resolution scanning electron microscopy","high resolution transmission electron microscopy","illumination","imaging","inductively coupled plasma atomic emission spectroscopy","inductively coupled plasma mass spectrometry","infrared imaging","laser diffraction","liight microscopy","liquid chromatography - mass spectrometry","liquid scintillation counting","magnetic property measurement","mass quantitation","matrix assisted laser desorption ionisation - time of flight","microfluidics","multi photon confocal laser scanning microscopy","multiphoton laser scanning microscopy","NNNN","nuclear magnetic resonance","particle quantitation","photoacoustic imaging","photoacoustic spectrometry","polymerase chain reaction","positron emission tomography","powder diffraction","protein quantitation","radioactivity quantiation","Raman spectroscopy","refractometry","scanning auger spectrometry","scanning electron microscopy","scanning probe microscopy","scanning transmission electron microscopy","scanning tunneling microscopy","sfaer927034wqw34","size exclusion chromatography with multi-angle laser light scattering","spectrofluorometry","spectrophotometry","surface plasmon resonance","SY-New-Technique-Type","sy-thurs-techtype-5","sy-thursday-techtype","temperature measurement","thermogravimetric analysis","time-resolved fluorescence microscopy ","transmission electron microscopy","wavelength dispersive spectroscopy","weight","X-ray diffraction","X-ray photoelectron spectroscopy","zeta potential analysis","other"],"manufacturerLookup":["ACT GmbH","Aerotech","Affymetrix","Agilent","Alltech","Amersham","Amersham Pharmacia Biotech","Applied Biosystems","Applied Precision","Asylum Research","B&W Tek","BD Biosciences","Beckman/Coulter","Becton Dickinson","Biacore","BioLogics","Biorad","BioTek","Brookhaven Instruments","Bruker","Budget Sensors","Caliper Life Sciences","Carl Zeiss","ChemoMetec","CPS Instruments","CTI Concorde Microsystems","Dako","Diagnostica Stago","Dynatech","EDAX","Endra","Eppendorf","FEI","FLIR","Gatan","GE Healthcare","Guava Technologies/Millipore","Hamamatsu","Hewlett-Packard","Hitachi","Horiba","Invitrogen","JEOL","Jobin Yvon","Kodak","Kratos Analytical","Labsystems","Lakeshore","LaVision BioTec","LECO","Leica","Luxtron","Malvern","Micromass","Micromeritics","Millipore","Molecular Devices","Molecular Imaging","Nikon","OBB Corp","Ocean Optics","Olympus","Packard","Panametrics","Park Systems","PerkinElmer","Phenomenex","Philips","Photal Otsuka","Photometrics","Photon Technology International","Picoquant","Point Electronic GmBh","Princeton Instruments","PSS","Quantachrome Instruments","Quantum Design","Renishaw","Rigaku","Roche Applied Science","RPMC Lasers","Sartorius","Shimadzu","Siemens Medical","Soft Imaging Systems","SY factory","sy-thurs-manuf-5","sy-thursday-inst-type","TA Instruments","Tecan","Test","Thermo Electron","Thermo Scientific","TosoHaas","Varian","Visualsonics","Waters","Wyatt Technologies","Zeiss","other"]},"finding":[{"findingId":100171777,"numberOfColumns":1,"numberOfRows":1,"rows":[{"cells":[{"value":"","datumOrCondition":"datum","columnOrder":null,"createdDate":null}]}],"columnHeaders":[{"columnName":"asd","conditionProperty":null,"valueType":null,"valueUnit":null,"columnType":"datum","displayName":"asd","constantValue":"","columnOrder":1,"createdDate":1411062444000}],"files":[],"theFile":{"uriExternal":false,"uri":"","type":"","title":"","description":"","keywordsStr":"","id":null,"createdBy":"","createdDate":null,"sampleId":"","errors":null,"externalUrl":"","theAccess":null,"isPublic":false},"theFileIndex":-1,"dirty":false,"errors":[],"parentCharType":"","parentCharName":""}],"analysisConclusion":null,"selectedOtherSampleNames":[],"copyToOtherSamples":false,"submitNewChar":false,"charTypesLookup":["physico-chemical characterization","in vitro characterization","in vivo characterization","Canano_char_type","SY-New-Char-Type","TEST111","ex vivo","other"],"protocolLookup":[{"domainId":69435392,"domainFileId":69304322,"domainFileUri":"http://www.sciencedirect.com/science/article/pii/S1045105608000572","displayName":"Physicochemical and biological assays for quality control of biopharmaceuticals: Interferon alfa-2 case study, version v1.0"},{"domainId":63340544,"domainFileId":63275009,"domainFileUri":"protocols/20140825_10-55-03-531_20110228_05-08-58-717_Apoptosis_Procedure.pdf","displayName":"Test File Protocol3"},{"domainId":31817728,"domainFileId":31686657,"domainFileUri":"protocols/20111018_12-08-22-229_protocols_20090113_11-11-33-967_NCL_Method_NIST-NCL_PCC-1.pdf","displayName":"Demo-PCC, version 1.0"},{"domainId":29949960,"domainFileId":29655055,"domainFileUri":"http://ncl.cancer.gov/NCL_Method_PCC-13.pdf","displayName":"NIST - NCL Joint Assay Protocol, PCC-13 (PCC-13), version 1.1"},{"domainId":29949959,"domainFileId":29655054,"domainFileUri":"http://ncl.cancer.gov/NCL_Method_PCC-12.pdf","displayName":"NIST - NCL Joint Assay Protocol, PCC-12 (PCC-12), version 1.1"},{"domainId":29949958,"domainFileId":29655053,"domainFileUri":"http://ncl.cancer.gov/NCL_Method_PCC-14.pdf","displayName":"NIST - NCL Joint Assay Protocol, PCC-14 (PCC-14), version 1.0"},{"domainId":29949957,"domainFileId":29655052,"domainFileUri":"http://ncl.cancer.gov/NCL_Method_PCC-11.pdf","displayName":"NIST - NCL Joint Assay Protocol, PCC-11 (PCC-11), version 1.1"},{"domainId":29949956,"domainFileId":29655050,"domainFileUri":"http://ncl.cancer.gov/NCL_Method_PCC-9.pdf","displayName":"NIST - NCL Joint Assay Protocol, PCC-9 (PCC-9), version 1.1"},{"domainId":29949955,"domainFileId":29655049,"domainFileUri":"http://ncl.cancer.gov/NCL_Method_PCC-8.pdf","displayName":"NIST - NCL Joint Assay Protocol, PCC-8 (PCC-8), version 1.1"},{"domainId":29949954,"domainFileId":29655048,"domainFileUri":"http://ncl.cancer.gov/NCL_Method_PCC-10.pdf","displayName":"NIST - NCL Joint Assay Protocol, PCC-10 (PCC-10), version 1.1"},{"domainId":29949953,"domainFileId":29655047,"domainFileUri":"http://ncl.cancer.gov/NCL_Method_PCC-7.pdf","displayName":"NIST - NCL Joint Assay Protocol, PCC-7 (PCC-7), version 1.1"},{"domainId":29949952,"domainFileId":29655046,"domainFileUri":"http://ncl.cancer.gov/NCL_Method_PCC-6.pdf","displayName":"NIST - NCL Joint Assay Protocol, PCC-6 (PCC-6), version 1.1"},{"domainId":25210112,"domainFileId":25177344,"domainFileUri":"http://ncl.cancer.gov/NCL_Method_PCC-1.pdf","displayName":"NIST - NCL Joint Assay Protocol, PCC-1 (PCC-1), version 1.1"}],"charSourceLookup":[{"id":64847872,"displayName":"BROWN_STANFORD"}],"otherSampleNameLookup":["BROWN_STANFORD-HLeeJNM2008-01","BROWN_STANFORD-HLeeJNM2008-01-copy","BROWN_STANFORD-HLeeJNM2008-02","BROWN_STANFORD-HLeeJNM2008-03","BROWN_STANFORD-HLeeJNM2008-04","BROWN_STANFORD-HLeeJNM2008-05","BROWN_STANFORD-JXieJACS2008-01","NCL-23-1123123217784","NCL-23-1123321312321321321321312","NCL-23-123232322232","NCL-23-1232323232323232323232","NCL-23-1232323299181881","NCL-23-1983989238981298219321","NCL-23-1983989238981298219321232","NCL-23-199288292","NCL-49-421323","NCL-49-43343434343443","SY-CANANOLAB-161","gsaeteateara","ncl-24-1-Copy-sy","slsldhafasd","sy-sprint-4-2","sy-sprint-4-3","testDSADJASKDSAJKJSDKASD"],"datumConditionValueTypeLookup":["boolean","mean","median","mode","number averaged","number of replicates","number of samples","observed","RMS","standard deviation","standard error of mean","SY-Test-Column-valuetype","sy-thurs-colvaltype-5","sy-thurs-colvaluetype-6","Z-average","Z-score","other"],"assayTypesByCharNameLookup":["physical state","other"],"errors":[],"messages":[],"dirtyFindingBean":null,"dirtyExperimentBean":null};
    // $scope.columnNameLookup = ["asd","other"];
    $scope.fileTypes = ["document","graph","image","movie","spread sheet"];


    // scrolls to section on page with provided id //
    $scope.scroll = function(id) {
        var old = $location.hash();
        $location.hash(id);
        $anchorScroll();
        $location.hash(old);
    };

    // popup calendar functions //
    $scope.setupCalendar = function() {
        $scope.today = function() {
            $scope.data.characterizationDate = new Date();
        };
        $scope.clear = function () {
            $scope.data.characterizationDate = null;
        };
        $scope.toggleMin = function() {
            $scope.minDate = $scope.minDate ? null : new Date();
        };
        // $scope.toggleMin();
        $scope.open = function($event) {
            $event.preventDefault();
            $event.stopPropagation();
            $scope.opened = true;
        };
        $scope.dateOptions = {
            formatYear: 'yy',
            startingDay: 1
        };
        $scope.initDate = new Date('2016-15-20');
        $scope.formats = ['dd-MMMM-yyyy', 'yyyy/MM/dd', 'dd.MM.yyyy', 'shortDate'];
        $scope.format = 'shortDate';
    };


    $scope.setupCalendar();

    // initial rest call to get master data object //
    if ($scope.isEdit) {
        // run initial rest call to setup characterization edit form //
        $scope.title = "Edit";
        $scope.saveButton = "Update";
        $http({method: 'GET', url: '/caNanoLab/rest/characterization/setupUpdate?sampleId='+$scope.sampleId+'&charId='+$scope.charId+'&charClassName='+$scope.charClassName+'&charType='+$scope.type}).
            success(function(data, status, headers, config) {
            $scope.data = data;
            $scope.sampleName = sampleService.sampleName($scope.sampleId);

            if (!$scope.data.characterizationDate) {
                $scope.data.characterizationDate = null;
            };
            $scope.loader = false;
            $scope.dataCopy = angular.copy($scope.data);
        }).
            error(function(data, status, headers, config) {
            $scope.loader = false;
        });
    }
    else {
        // run initial rest call to setup characterization add form //
        $scope.title = "Add";
        $scope.saveButton = "Submit";
        $http({method: 'GET', url: '/caNanoLab/rest/characterization/setupAdd?sampleId='+$scope.sampleId+'&charType='+$scope.type}).
            success(function(data, status, headers, config) {
            $scope.data = data;
            $scope.sampleName = sampleService.sampleName($scope.sampleId);

            // $scope.today();
            $scope.loader = false;
            $scope.dataCopy = angular.copy($scope.data);
        }).
            error(function(data, status, headers, config) {
            $scope.loader = false;
        });
    };


    if($scope.data.characterizationDate) {
        $scope.data.characterizationDate = new Date($scope.data.characterizationDate);
    };

    $http({method: 'GET', url: 'rest/characterization/getDatumNumberModifier?columnName=Number%20Modifier'}).
        success(function(data, status) {
        $scope.operands = data;
        if (data.includes("other")) {
            var index = data.indexOf("other");
            data.splice(index,1);
            $scope.operands = data;
        };

    });



    // gets characterization names when characterization type dropdown is changed //
    $scope.characterizationTypeDropdownChanged = function() {
        $scope.data.assayTypesByCharNameLookup = [];
        delete $scope.data.assayType;
        delete $scope.data.name;
        delete $scope.data.protocolId;
        delete $scope.data.characterizationSourceId;
        delete $scope.domainFileUri;

        $scope.loader = true;
        $http({method: 'GET', url: '/caNanoLab/rest/characterization/getCharNamesByCharType?charType='+$scope.data.type}).
            success(function(data, status, headers, config) {
            $scope.data.charNamesForCurrentType = data;
            $scope.loader = false;
        }).
            error(function(data, status, headers, config) {
            $scope.loader = false;
        });
    };

    // gets assay types when characterization name dropdown is changed //
    $scope.characterizationNameDropdownChanged = function() {
        delete $scope.data.assayType;

        $scope.loader = true;
        $http({method: 'GET', url: '/caNanoLab/rest/characterization/getAssayTypesByCharName?charName='+ $scope.data.name}).
            success(function(data, status, headers, config) {
            $scope.data.assayTypesByCharNameLookup = data;
            $scope.loader = false;
        }).
            error(function(data, status, headers, config) {
            $scope.loader = false;
        });

        // gets property block //
        $http({method: 'GET', url: '/caNanoLab/rest/characterization/getCharProperties?charName='+ $scope.data.name}).
            success(function(data, status, headers, config) {
            $scope.data.property = data;
        }).
            error(function(data, status, headers, config) {
            $scope.loader = false;
        });
    };

    // looks to see if technique type has abbreviation //
    $scope.techniqueTypeInstrumentDropdownChanged = function() {
        $http({method: 'GET', url: '/caNanoLab/rest/characterization/getInstrumentTypesByTechniqueType?techniqueType='+$scope.techniqueInstrument.techniqueType}).
            success(function(data, status, headers, config) {
            $scope.instrumentTypesLookup = data;
        }).
            error(function(data, status, headers, config) {
        });
    };

    // gets URL for protocol name //
    $scope.getDomainFileUri = function() {
        for (var x = 0; x < $scope.data.protocolLookup.length;x++) {
            if ($scope.data.protocolId==$scope.data.protocolLookup[x].domainId) {
                $scope.domainFileUri = $scope.data.protocolLookup[x].domainFileUri;
            };
        };
    };

    // open new experiment section //
    $scope.openNewExperimentConfig = function() {
        $scope.updateExperimentConfig = 1;
        $scope.isNewExperimentConfig = 1;
        $scope.techniqueInstrument = {};
        $scope.techniqueInstrument.instruments = [];
        $scope.scroll('editTechniqueInstrumentInfo');
    };

    // open update experiment section //
    $scope.openUpdateExperimentConfig = function(item) {
        $scope.updateExperimentConfig = 1;
        $scope.isNewExperimentConfig = 0;
        $scope.techniqueInstrument = item;
        $http({method: 'GET', url: '/caNanoLab/rest/characterization/getInstrumentTypesByTechniqueType?techniqueType='+$scope.techniqueInstrument.techniqueType}).
            success(function(data, status, headers, config) {
            $scope.instrumentTypesLookup = data;
        }).
            error(function(data, status, headers, config) {
        });
        // $scope.instrumentTypesLookup = ["control module","guard column","multi angle light scattering detector","photometer","refractometer","separation column","separations module","spectrophotometer","other"];
        $scope.techniqueInstrumentCopy = angular.copy(item);
        $scope.scroll('editTechniqueInstrumentInfo');
    };

    // save experiment config and close section //
    $scope.saveExperimentConfig = function() {
        $scope.loader = true;
        if ($scope.isNewExperimentConfig) {
            $scope.data.techniqueInstruments.experiments.push($scope.techniqueInstrument);
        };
        $scope.techniqueInstrument.parentCharType = $scope.data.type;
        $scope.techniqueInstrument.parentCharName = $scope.data.name;
        $scope.techniqueInstrument.dirty = 1;
        $http({method: 'POST', url: '/caNanoLab/rest/characterization/saveExperimentConfig',data: $scope.data}).
        success(function(data, status, headers, config) {
            $scope.saveButton = "Update";
            $scope.loader = false;
            $scope.data=data;
        }).
        error(function(data, status, headers, config) {
            $scope.loader = false;
        });
        $scope.updateExperimentConfig = 0;
    };

    // removes experiment config and close section //
    $scope.removeExperimentConfig = function() {
        $scope.loader = true;
        $http({method: 'POST', url: '/caNanoLab/rest/characterization/removeExperimentConfig',data: $scope.techniqueInstrument}).
        success(function(data, status, headers, config) {
            $scope.loader = false;
            $scope.data=data;
        }).
        error(function(data, status, headers, config) {
            $scope.loader = false;
        });
        $scope.updateExperimentConfig = 0;
    };

    // cancel experiment config and close section //
    $scope.cancelExperimentConfig = function() {
        $scope.updateExperimentConfig = 0;
        angular.copy($scope.techniqueInstrumentCopy,$scope.techniqueInstrument);
        $scope.updateInstrument = 0;

    };

    // open new instrument section //
    $scope.openNewInstrument = function() {
        $scope.updateInstrument = 1;
        $scope.instrument = {"manufacturer":null,"modelName":null,"type":null};
        $scope.isNewInstrument = 1;
    };

    // open existing instrument section //
    $scope.openExistingInstrument = function(instrument) {
        $scope.updateInstrument = 1;
        $scope.instrument = instrument;
        $scope.isNewInstrument = 0;
        $scope.instrumentCopy = angular.copy(instrument);
        if ($scope.instrumentTypesLookup.indexOf($scope.instrument.type)==-1) {
            $scope.instrument.type="";
        };
    };

    // saves instrument. checks if it is new. if it is add it to techniqueInstrument //
    $scope.saveInstrument = function(instrument) {
        if ($scope.isNewInstrument) {
            $scope.techniqueInstrument.instruments.push($scope.instrument);
        };
        $scope.updateInstrument = 0;

    };

    // removes instrument from list //
    $scope.removeInstrument = function(instrument) {
        $scope.techniqueInstrument.instruments.splice($scope.techniqueInstrument.instruments.indexOf(instrument),1);
        $scope.updateInstrument = 0;
    };

    // closes instrument section, reset instrument if it exists //
    $scope.cancelInstrument = function(instrument) {
        $scope.updateInstrument = 0;
        angular.copy($scope.instrumentCopy, $scope.instrument);
    };

    // opens new finding dialog //
    $scope.addNewFinding = function() {
        var old = $location.hash();
        $scope.currentFinding = {'columnHeaders':[]};
        $scope.currentFinding.dirty = 1;
        $scope.updateFinding = 1;
        $scope.finding = {};
        $scope.scroll('editFindingInfo');
        $scope.isNewFinding = 1;
        $scope.currentFindingCopy = angular.copy($scope.currentFinding);

    };

    // open finding dialog with existing finding //
    $scope.updateExistingFinding = function(finding) {
        var old = $location.hash();
        $scope.updateFinding = 1;
        $scope.currentFinding = finding;
        checkAllFindingCells();
        $scope.scroll('editFindingInfo');
        $scope.isNewFinding = 0;
        $scope.currentFinding.dirty = 1;
        $scope.currentFindingCopy = angular.copy(finding);
    };

    // updates rows and columns and runs rest call update //
    // Check that input is valid for it's column type.
    $scope.updateRowsColsForFinding = function() {
        $scope.loader = true;
        $scope.badFindingCell = createArray(csvDataColCount, csvDataRowCount);

        $http({method: 'POST', url: '/caNanoLab/rest/characterization/updateDataConditionTable',data: $scope.currentFinding}).
        success(function(data, status, headers, config) {
            if( data.rows[csvDataRowCount - 1] === undefined ){
                csvDataRowCount = data.numberOfRows;
            }

            for( var y = 0; y < csvDataRowCount; y++){

                for (var x = 0; x < csvDataColCount; x++) {
                    // If the user has reduced the number of columns, make sure we don't try to update columns that no longer exist.
                    if( (data.rows[y].cells[x] !== null) && (data.rows[y].cells[x] !== undefined)) {
                        data.rows[y].cells[x].value = Object(csvDataObj[y][x]);
                        if (x < $scope.currentFinding.length) {
                            data.rows[y].cells[x].datumOrCondition = $scope.currentFinding.columnHeaders[x].columnType;
                        }
                        // When the column type is set or reset, check all cell contents for valid entries for each column, one row at a time.
                        if (x < $scope.currentFinding.columnHeaders.length) {
                            $scope.badFindingCell[x][y] = validateFindingCellInput($scope.currentFinding.columnHeaders[x].columnType,
                                data.rows[y].cells[x].value);
                        }
                        // If there are fewer column types/header set than there are columns.
                        // Data put in a cell with a column that does not have it's type set is never considered invalid.
                        else {
                            $scope.badFindingCell[x][y] = false;
                        }
                    }
                }
            }

            // If there are already column headers set, preserve them.
            for( var colX = 0; colX < csvDataColCount; colX++){
                if( ($scope.currentFinding.columnHeaders[colX] !== null) &&($scope.currentFinding.columnHeaders[colX] !== undefined)){
                    data.columnHeaders[colX] = $scope.currentFinding.columnHeaders[colX];
                }
            }

            $scope.loader = false;
            $scope.currentFinding=data;

        }).
        error(function(data, status, headers, config) {
            $scope.loader = false;
        });
    };

    // opens column form to change properties for column //
    $scope.openColumnForm = function(cell) {
        $scope.findingsColumn = cell;
        $scope.columnForm = 1;

        $scope.findingsColumnCopyForRestore =  {
            "columnName":"Column 1",
            "conditionProperty":null,
            "valueType":null,
            "valueUnit":null,
            "columnType":null,
            "displayName":"Column 1",
            "constantValue":"",
            "columnOrder":1,
            "createdDate":null
        };

        $scope.findingsColumnCopy = angular.copy($scope.findingsColumn);

        if ($scope.findingsColumn.columnType) {
            $scope.onColumnTypeDropdownChange(1);
        }
    };

    // In the HTML the user is limited only numbers, and in a range from 1 to column count.
    // Here we need to add the limitation of, no duplicate numbers.
    $scope.columnOrderChanged = function(numberInput, index){
        $scope.disableChangeColumnOrder = false;
        for( let i0 = 0; i0 < ( $scope.currentFinding.columnHeaders.length - 1); i0++){
            for( let i1 = ( i0 + 1 ); i1 < $scope.currentFinding.columnHeaders.length; i1++){
                if( $scope.currentFinding.columnHeaders[i0].columnOrder === $scope.currentFinding.columnHeaders[i1].columnOrder){
                    $scope.disableChangeColumnOrder = true;
                    return;
                }
            }
        }
    };


    /**
     * Called when Finding cell input changes.
     *
     * @param textInput   Text input from the HTML.
     */
    $scope.currentFindingCellChanged = function(textInput){
        let xy = textInput.id.split(":");
        $scope.badFindingCell[xy[0]][xy[1]] = validateFindingCellInput( $scope.currentFinding.columnHeaders[xy[0]].columnType, textInput.value);
    };

    /**
     * Check for valid cell data for column type
     * @param colType
     * @param cellData
     * @returns True on bad cell data for column type.
     */
    function validateFindingCellInput( colType, cellData){
        if( ( colType === null) || ( colType === undefined)){
            return false;
        }
       return (
           (colType === 'datum') &&
           ( cellData !== null) && (isNaN(cellData.replace(/(d|f)$/, '')))
       );
    }

    /**
     * Check each cell for valid data for column type and set status in $scope.badFindingCell array.
     */
    function checkAllFindingCells() {
        let rowCount = $scope.currentFinding.rows.length;
        if (rowCount > 0) {
            let cellCount = $scope.currentFinding.rows[0].cells.length;
            $scope.badFindingCell = createArray(cellCount, rowCount);
            for (let y = 0; y < rowCount; y++) {
                for (let x = 0; x < cellCount; x++) {
                    $scope.badFindingCell[x][y] = validateFindingCellInput($scope.currentFinding.rows[y].cells[x].datumOrCondition,
                        $scope.currentFinding.rows[y].cells[x].value);
                }
            }
        }
    }


     /**
      * Create multi dimensional array.
      * @param len  The length of one or more dimensions of array
      * @returns {any[]}  The array
      */
     function createArray(len) {
         let arr = new Array(len || 0), i = len;
         if (arguments.length > 1) {
             var args = Array.prototype.slice.call(arguments, 1);
             while(i--) arr[len-1 - i] = createArray.apply(this, args);
         }
            return arr;
     }


     /**
      * This is called when a file in the users local machine is selected for csv upload.
      *
      * @param v File data.
      */
     $scope.fileNameChanged = function(v) {
         dataReaderReadFile(0, v[0].size);
     };


     /**
      * Called by the call back, get the file data here.
      *
      * @param opt_startByte
      * @param opt_stopByte
      */
     var dataReaderReadFile = function (opt_startByte, opt_stopByte) {
         let files = document.getElementById('csvFile').files;
         let reader = new FileReader();
         reader.onloadend = function (evt) {
             csvDataObj = parseCsv(evt.target.result);

             // Did we get a parse error
             if(csvDataObj === null){
                 console.error('CSV import parse error: ', csvImportError);
                 alert( 'CSV import parse error: ' + csvImportError);
                 return;
             }

             // Get dimensions of csv data.
             csvDataColCount = 0;
             csvDataRowCount = csvDataObj.length;
             for (var y = 0; y < csvDataRowCount; y++) {
                 if( csvDataObj[y].length > csvDataColCount){
                     csvDataColCount = csvDataObj[y].length;
                 }
             }
             $scope.currentFinding.numberOfColumns = csvDataColCount;
             $scope.currentFinding.numberOfRows = csvDataRowCount;

             $scope.updateRowsColsForFinding();
         };
        reader.readAsBinaryString(files[0].slice(0, files[0].size));
     };


    /**
     * Return true if: not too many rows, no row is too long, and csv format is correct.
     *
     * @param csv
     * @returns {boolean}
     */
    let validateCsv = function( csv){
        // Normalize line feeds
        let temp = (csv.replace(/\r\n/g, '\r').replace(/\n\r/g, '\r').replace(/\n/g, '\r')).split(/\r/);

        // Do we have too many rows?
        if( temp.length > csvMaxNumberOfLines){
            csvImportError = 'Too many Lines (' + temp.length + ')';
            return false;
        }

        // Are any cells too long?
        // Determine length of longest cell entry
        let biggestLine = 0;
        for( let f0 = 0; f0 < temp.length; f0++){
            if( biggestLine < temp[f0].length){
                biggestLine = temp[f0].length;
            }
        }

        // If at least one entry is too long, set error and return false
        if( biggestLine > csvMaxLenOfEntry){
            console.error('ERROR line(s) too long (' + biggestLine + ')');
            csvImportError = 'line(s) too long (' + biggestLine + ')';
            return false;
        }


        // Send each line to csv validation function.
        // Remove anything that is not a quote or a comma. That is all we need for validating csv.
        let regex = new RegExp('[^",]', 'g');
        for( let f = 0; f < temp.length; f++){
            let csvString = temp[f].replace(regex, '');
            let isValid = validateCsvLine(csvString);
            if( ! isValid ){
                return false;
            }
        }

        // Return true if: not too many rows, no row is too long, and csv format is correct.
        return true;
    };


    /**
     * Check for correctly nested quotes.
     *
     * @param csvLine
     * @returns {boolean}
     */
    function validateCsvLine( csvLine ) {
        let inQ = false;
        let badData = false;
        for( let f = 0; f < csvLine.length; f++){
            if( ! inQ ){

                // A starting quote plus a nested quote (3 quotes)
                if( (csvLine.length <= (f+2)) && csvLine[f] ==='"' && csvLine[f+1] ==='"' && csvLine[f+2] ==='"'){
                    inQ = true;
                }

                // Two quotes, BUT not in a quote, and ends.
                else if( (csvLine.length <= (f+1)) && csvLine[f] ==='"' && csvLine[f+1] ==='"'){
                    badData = false;
                    break;
                }
                else if(csvLine[f] ==='"'){
                    inQ = true;
                }
            }
            else{
                // An ending quote
                if( csvLine[f] ==='"' && csvLine[f+1] !=='"' ){
                    inQ = false;
                }
                else if( csvLine[f] ==='"' && csvLine[f+1] ==='"')
                {
                    f++;
                }
            }
        }

        // Are we still in a quote at the end
        if(inQ){
            badData = true;
            csvImportError = 'csv validation error';
        }
        return (! badData );
    }


        /**
         * Convert csv data to javascript array.
         *
         * @param data
         * @returns {*}   javascript array.
         */
    function parseCsv(data) {
        if( ! validateCsv(data) ){
            return null;
        }

        // Split on the CR or LF
        let dataLines = qFix(data.replace(/\r\n/g, '\r').replace(/\n\r/g, '\r').replace(/\n/g, '\r')).split(/\r/);
        let startCell = 1; //true
        let currentCell = '';
        let currentCellType = 0; // 0=unknown  1=comma no double quote  2=comma with double quote
        let i = 0;
        let csvData;
        let csvDataObj = [];

        for (let dataLine = 0; dataLine < dataLines.length && runaway > 0; dataLine++){
            csvData = dataLines[dataLine];

            if (csvData.length < 1) {
                continue;
            }

            let lineOfValues = [];
            i = 0;
            while (i < csvData.length && runaway > 0) {
                let trailingCommas = [];
                trailingCommas = csvData.match(/(,+)$/g);
                if (trailingCommas !== null) {
                    let replacementStr = '';
                    for (let f = 0; f < trailingCommas[0].length; f++) {
                        replacementStr += ',""';
                    }
                    let re = new RegExp(trailingCommas[0] + '$');
                    csvData = csvData.replace(re, replacementStr);
                }
                // Determine cell type
                if (csvData.substr(i, 1) === '"') {
                    currentCellType = 2;
                }
                else {
                    currentCellType = 1;
                }

                if (currentCellType === 1) {
                    // Just grab to the first comma
                    currentCell = csvData.substr(i).match(/[^,]*,/);
                    if (currentCell !== null) {
                        currentCell = currentCell[0];
                        lineOfValues.push(cleanCsvValue(currentCell));
                    }
                    // No comma, we are at the end.
                    else {
                        currentCell = csvData.substr(i);
                        lineOfValues.push(cleanCsvValue(currentCell));
                    }
                    i += currentCell.length;
                } else if (currentCellType === 2) {
                    csvData = csvData.substr(i);
                    i = 0;
                    startCell = 1;
                    let charStatus = 0;  // Nothing yet
                    let currentChar = '';
                    let currentNextChar = '';
                    let i1 = 0;

                    while (i1 < csvData.length) {
                        currentChar = csvData.substr(i1, 1);
                        if (i1 + 1 < csvData.length) {
                            currentNextChar = csvData.substr(i1 + 1, 1);
                        }
                        else {
                            currentNextChar = '';
                        }
                        i1++;

                        // The first char
                        if (charStatus === 0 && startCell === 1) {
                            // Is it a quote (it should be)
                            if (currentChar === '"') {
                                charStatus = 1;  // We have seen the first quote
                            }
                            startCell = 0;  // No longer looking at the first char
                            currentChar = csvData.substr(i1, 1);
                            if (i1 + 1 < csvData.length) {
                                currentNextChar = csvData.substr(i1 + 1, 1);
                            }
                            else {
                                currentNextChar = '';
                            }
                        }  // END if (charStatus === 0 && startCell === 1)

                        // Not the first char
                        else if (startCell !== 1) {
                            // We are past the first quote
                            if (charStatus === 1) {  // We have seen the first quote
                                // Check for two double quotes, this is a quote within a quoted cell - ignore it and go past it
                                if (currentChar === '"' && currentNextChar === '"') {
                                    i1 += 1;
                                }
                                // A quote here means the end
                                else if (currentChar === '"') {
                                    // Find the next comma or the end of the line.
                                    currentCell = csvData.substr(0, i1);
                                    csvData = csvData.substr(currentCell.length + 1);
                                    i1 = 0;
                                    startCell = 1; //true
                                    charStatus = 0;
                                    lineOfValues.push(cleanCsvValue(currentCell));
                                }
                            }

                        }  // END else if( startCell !== 1)
                            runaway--;
                    }
                }
                runaway--;
            } // End while loop

            csvDataObj.push(lineOfValues);

            runaway--;

        } // End for loop

        // Check here for too may columns
        if( getMaxColumnCount( csvDataObj) > csvColumnMaxCount ){
            csvImportError = 'Too many columns (' + getMaxColumnCount( csvDataObj) +')';
            return null;
        }
            let columnCount =  getMaxColumnCount(csvDataObj);
            for( let f = 0; f < csvDataObj.length; f++){
                while( csvDataObj[f].length < columnCount){
                    csvDataObj[f].push('');
                }
            }
            return csvDataObj;
    }

    /**
     * Returns number of columns, it is possible for csv data to have inconsistent column count for rows
     * @param csvData
     * @returns {number}  The column count for the row(s) with the most columns.
     */
    function getMaxColumnCount( csvData){
        var columnCount = 0;
        for( var row = 0; row < csvData.length; row++ ){
            if( columnCount < csvData[row].length){
                columnCount = csvData[row].length;
            }
        }
        return columnCount;
    }


    /**
     * Clean up Unicode type quotes and some UTF-8 chars
     *
     * @param input
     * @returns {string}
     */
    function qFix(input) {
        var output = '';
        for (var i = 0; i < input.length; ++i) {

            if (input.charCodeAt(i) === 226) {
                // Unicode double quote
                if (
                    (input.charCodeAt(i + 1) === 128 && input.charCodeAt(i + 2) === 157) ||
                    (input.charCodeAt(i + 1) === 128 && input.charCodeAt(i + 2) === 156)
                ) {
                    i += 2;
                    output += '"';
                }
                // Unicode single quote
                else if ( input.charCodeAt(i + 1) === 128 && input.charCodeAt(i + 2) === 153) {
                    i += 2;
                    output += '\'';
                }

            } else if ( input.charCodeAt(i) === 194 || input.charCodeAt(i) === 195 ){
                var hexDigit0 = input.charCodeAt(i).toString(16);
                if (hexDigit0.length % 2) {
                    hexDigit0 = '0' + hexDigit0;
                }
                var hexDigit1 = input.charCodeAt(i+1).toString(16);
                if (hexDigit1.length % 2) {
                    hexDigit1 = '0' + hexDigit1;
                }
                var hex = '%' + hexDigit0 +'%' + hexDigit1;
                let decoded = decode_utf8(hex);
                if( decoded === 'ERROR-ERROR'){
                    console.error('ERROR ');
                    output = '';
                    return output;
                }
                output += decoded;
                i++;
            }
            else {
                output += input[i];
            }
        }
        return (output);
    };


    function cleanCsvValue(val) {
        if (val.substr(0, 1) === '"') {
                val = val.substr(1);
        }
        if (val.substr(val.length - 1) === ',') {
            val = val.substr(0, val.length - 1);
        }
        if (val.substr(val.length - 1) === '"') {
            val = val.substr(0, val.length - 1);
        }

        val = val.replace(/""/g, '"');
        return val;
    };


    /**
     *
     * @param s
     * @returns {string}
     */
    function decode_utf8(s) {
        var returnData = '';
        try {
            returnData = decodeURIComponent(s);
        }
        catch (e) {
            returnData = 'ERROR-ERROR'; // TODO  Make this a const
            console.error('ERROR: ' , e);
        }
        return returnData;
    }


        // close column form without saving //
    $scope.closeColumnForm = function() {
        angular.copy($scope.findingsColumnCopy, $scope.findingsColumn);
        $scope.columnForm = 0;
    };

    // close column form with saving //
    $scope.saveColumnForm = function() {
        $scope.columnForm = 0;

        var columnIndex = 0;
        if( $scope.findingsColumn.columnOrder != null ) {
            columnIndex = parseInt($scope.findingsColumn.columnOrder) - 1;
            for (var x=0;x<$scope.currentFinding.rows.length;x++) {
                var curCell = $scope.currentFinding.rows[x].cells[columnIndex];
                if ($scope.findingsColumn.constantValue!='') {
                    curCell.value=$scope.findingsColumn.constantValue;
                }
            };
            var headerName = $scope.findingsColumn.columnName;

            if( $scope.findingsColumn.valueType != null ) {
                headerName = headerName + ' (' + $scope.findingsColumn.valueType + ')';
            }
            $scope.currentFinding.columnHeaders[columnIndex].displayName = headerName;

        }

        // Check here for valid cell data with the new column type.
        initDatumOrCondition();
        checkAllFindingCells();

    };

    function initDatumOrCondition() {
        var rowCount = $scope.currentFinding.rows.length;
        var cellCount = $scope.currentFinding.rows[0].cells.length;
        for (var y = 0; y < rowCount; y++) {
            for (var x = 0; x < cellCount; x++) {
                $scope.currentFinding.rows[y].cells[x].datumOrCondition = $scope.currentFinding.columnHeaders[x].columnType;
            }
        }
    }



        // remove column data //
    $scope.removeColumnForm = function() {
         resetColumnConfirmDialog = $modal.open({
            templateUrl: 'views/sample/view/dataColumnHeaderReset.html',
            controller: 'DataColumnHeaderResetCtrl',
            size: 'sm',
            resolve: {

                col: function () {
                    return $scope.findingsColumn;
                }
            }
        });

        resetColumnConfirmDialog.result.then(function (closeType) {
            if( closeType){
                angular.copy($scope.findingsColumnCopyForRestore, $scope.findingsColumn);
                $scope.columnForm = 0;
            }
        });
    };

    // opens column form to change order for columns. Does not actually order columns //
    $scope.openColumnOrderForm = function() {
        $scope.columnOrder = 1;
    };

    // called when columnType dropdown is changed on column form //
    $scope.onColumnTypeDropdownChange = function(newOpen) {
        $http({method: 'GET', url: '/caNanoLab/rest/characterization/getColumnNameOptionsByType?columnType='+$scope.findingsColumn.columnType+'&charName='+$scope.data.name+'&assayType='+$scope.data.assayType}).
            success(function(data, status, headers, config) {
            $scope.columnNameLookup = data;
            $scope.loader = false;
        }).
            error(function(data, status, headers, config) {
            $scope.loader = false;
        });

        if (newOpen) {
            $scope.getColumnValueUnitOptions();
        };
    };

    // gets column value unit options based on  name and condition //
    $scope.getColumnValueUnitOptions = function() {
       $http({method: 'GET', url: '/caNanoLab/rest/characterization/getColumnValueUnitOptions?columnName='+$scope.findingsColumn.columnName+'&conditionProperty='+$scope.findingsColumn.conditionProperty}).
            success(function(data, status, headers, config) {
            $scope.valueUnitsLookup = data;
            $scope.loader = false;
        }).
            error(function(data, status, headers, config) {
            $scope.loader = false;
        });
    };

    // called when columnType dropdown is changed on column form //
    $scope.onColumnNameDropdownChange = function() {
        $scope.getColumnValueUnitOptions();
        $http({method: 'GET', url: '/caNanoLab/rest/characterization/getConditionPropertyOptions?columnName='+$scope.findingsColumn.columnName}).
            success(function(data, status, headers, config) {
            $scope.conditionTypeLookup = data;
            $scope.loader = false;
        }).
            error(function(data, status, headers, config) {
            $scope.loader = false;
        });
    };

    // sets the column order //
    $scope.updateColumnOrder = function() {
        $scope.loader = true;


        $http({method: 'POST', url: '/caNanoLab/rest/characterization/setColumnOrder',data: $scope.currentFinding}).
        success(function(data, status, headers, config) {
            $scope.loader = false;
            $scope.currentFinding=data;
            $scope.columnOrder = 0;

            initDatumOrCondition();
            checkAllFindingCells();
        }).
        error(function(data, status, headers, config) {
            $scope.loader = false;
            $scope.columnOrder = 0;
        });
    };

    // saves finding info //
    $scope.saveFindingInfo = function() {

        $scope.loader = true;
        if ($scope.isNewFinding) {
            $scope.data.finding.push($scope.currentFinding);
        };

        for (var x=0; x<$scope.data.finding.length;x++) {
            if ($scope.data.finding[x].findingId==$scope.currentFinding.findingId) {
                $scope.data.finding.splice(x,1);
                $scope.data.finding.push($scope.currentFinding);
                break;
            }
        };


        let haveDatum = false;
        for( let i0=0; i0 < $scope.data.finding.length; i0++) {
            for( let i1 = 0; i1 <  $scope.data.finding[i0].columnHeaders.length; i1++ ){
                if( $scope.data.finding[i0].columnHeaders[i1].columnType === 'datum'){
                    haveDatum = true;
                    break;
                }
            }
            if( haveDatum ){
                break;
            }
        }

        if( ! haveDatum ){
            resetColumnConfirmDialog = $modal.open({
                templateUrl: 'views/sample/view/noDatumColumn.html',
                controller: 'NoDatumColumnCtrl',
                size: 'sm',
            });

            $scope.loader = false;
            return;
        }

        $http({method: 'POST', url: '/caNanoLab/rest/characterization/saveFinding',data: $scope.data}).
        success(function(data, status, headers, config) {

            $scope.saveButton = "Update";
            $scope.loader = false;
            $scope.data=data;
        }).
        error(function(data, status, headers, config) {
            $scope.loader = false;
            alert(data[0]);
        });
        $scope.updateFinding = 0;
    };

    // removes finding info //
    $scope.removeFindingInfo = function() {
    	if (confirm("Delete the Finding?")) {
    		$scope.loader = true;
            $http({method: 'POST', url: '/caNanoLab/rest/characterization/removeFinding',data: $scope.currentFinding}).
            success(function(data, status, headers, config) {
                $scope.loader = false;
                $scope.data=data;
            }).
            error(function(data, status, headers, config) {
                $scope.loader = false;
            });
    		$scope.updateFinding = 0;
    	}
    };

    $scope.cancelFindingInfo = function() {
        $scope.updateFinding = 0;
        angular.copy($scope.currentFindingCopy, $scope.currentFinding);
    };

    // deletes data and condition row //
    $scope.deleteDataConditionRow = function(row) {
        $scope.currentFinding.rows.splice($scope.currentFinding.rows.indexOf(row),1);
    };

    // save characterization record. //
    $scope.save = function() {
        $scope.loader = true;
        $http({method: 'POST', url: '/caNanoLab/rest/characterization/saveCharacterization',data: $scope.data}).
        success(function(data, status, headers, config) {
            $location.path("/editCharacterization").search({'sampleId':$scope.sampleId,'charMessage':'Characterization Saved'}).replace();
            $scope.loader = false;
            $scope.sampleMessage = 'Characterization Saved';
        }).
        error(function(data, status, headers, config) {
            $scope.loader = false;
        });
    };

    // remove characterization record. //
    $scope.remove = function() {
        $scope.loader = true;
        $http({method: 'POST', url: '/caNanoLab/rest/characterization/removeCharacterization',data: $scope.data}).
        success(function(data, status, headers, config) {
            $scope.loader = false;
            $location.path("/editCharacterization").search({'sampleId':$scope.sampleId}).replace();
        }).
        error(function(data, status, headers, config) {
            $scope.loader = false;
        });
    };

    // reset form //
    $scope.reset = function() {
        $scope.data = angular.copy($scope.dataCopy);
        $scope.domainFileUri = "";
        $scope.updateExperimentConfig = 0;
    };

    /* File Section */
    $scope.onFileSelect = function($files) {
        $scope.selectedFiles = [];
        $scope.selectedFiles = $files;


        if ($scope.selectedFiles != null && $scope.selectedFiles.length > 0 )
        	$scope.selectedFileName = $scope.selectedFiles[0].name;

        $scope.dataUrls = [];
		for ( var i = 0; i < $files.length; i++) {
			var $file = $files[i];
			if ($scope.fileReaderSupported && $file.type.indexOf('image') > -1) {
				var fileReader = new FileReader();
				fileReader.readAsDataURL($files[i]);
				var loadFile = function(fileReader, index) {
					fileReader.onload = function(e) {
						$timeout(function() {
							$scope.dataUrls[index] = e.target.result;
						});
					}
				}(fileReader, i);
			}
		}
    };

    $scope.editFile = function(fileId) {
    	$scope.selectedFileName = '';
        for (var k = 0; k < $scope.currentFinding.files.length; ++k) {

            var element = $scope.currentFinding.files[k];
            if (element.id == fileId ) {
                $scope.fileForm.externalUrl = element.externalUrl;
                $scope.fileForm.uri = element.uri;
                $scope.fileForm.uriExternal = element.uriExternal;
                $scope.fileForm.type = element.type;
                $scope.fileForm.title = element.title;
                $scope.fileForm.keywordsStr = element.keywordsStr;
                $scope.fileForm.description = element.description;
                $scope.fileForm.id = element.id;
                $scope.fileForm.createdBy = element.createdBy;
                $scope.fileForm.createdDate = element.createdDate;

                $scope.addNewFile = true;


                if( $scope.fileForm.externalUrl != null && $scope.fileForm.externalUrl != '') {
                    $scope.externalUrlEnabled = true;
                    $scope.fileForm.uriExternal = 'true';
                }
                else {
                    $scope.externalUrlEnabled  = false;
                    $scope.fileForm.uriExternal = 'false';
                }

                break;
            }
        }
    }

    $scope.removeFile = function(fileId) {
        if (confirm("Are you sure you want to delete the File?")) {
            $scope.loader = true;

            for (var k = 0; k < $scope.currentFinding.files.length; ++k) {
                var element = $scope.currentFinding.files[k];
                if (element.id == $scope.fileForm.id) {
                    $scope.currentFinding.files.splice(k,1);
                    $scope.currentFinding.theFileIndex = k;
                }
            }
            //$scope.currentFinding.files = $scope.files;

            if( $scope.currentFinding.theFile == null ) {
                $scope.currentFinding.theFile = {};
            }

            $scope.currentFinding.theFile.externalUrl = $scope.fileForm.externalUrl;
            $scope.currentFinding.theFile.uri = $scope.fileForm.uri;
            $scope.currentFinding.theFile.uriExternal = $scope.fileForm.uriExternal;
            $scope.currentFinding.theFile.type = $scope.fileForm.type;
            $scope.currentFinding.theFile.title = $scope.fileForm.title;
            $scope.currentFinding.theFile.keywordsStr = $scope.fileForm.keywordsStr;
            $scope.currentFinding.theFile.description = $scope.fileForm.description;
            $scope.currentFinding.theFile.id = $scope.fileForm.id;
            $scope.currentFinding.theFile.theAccess = $scope.fileForm.theAccess;
            $scope.currentFinding.theFile.isPublic = $scope.fileForm.isPublic;
            $scope.currentFinding.theFile.createdBy = $scope.fileForm.createdBy;
            $scope.currentFinding.theFile.createdDate = $scope.fileForm.createdDate;

            if( $scope.sampleId != null ) {
                $scope.currentFinding.theFile.sampleId = $scope.sampleId;
            }


            $http({method: 'POST', url: '/caNanoLab/rest/characterization/removeFile',data: $scope.currentFinding}).
                success(function(data, status, headers, config) {
                	$scope.currentFinding = data;
                    $scope.addNewFile = false;
                    $scope.loader = false;
                }).
                error(function(data, status, headers, config) {
                    // called asynchronously if an error occurs
                    // or server returns response with an error status.
                    // $rootScope.sampleData = data;
                    $scope.loader = false;
                    $scope.data.errors = data;
                });
        }
    };

    $scope.saveFile = function() {
        $scope.loader = true;
        var index = 0;
        $scope.upload = [];
        if ($scope.selectedFiles != null && $scope.selectedFiles.length > 0 ) {
            $scope.selectedFileName = $scope.selectedFiles[0].name;
            $scope.upload[index] = $upload.upload({
                url: uploadUrl,
                method: 'POST',
                headers: {'my-header': 'my-header-value'},
                data : $scope.fileForm,
                file: $scope.selectedFiles[index],
                fileFormDataName: 'myFile'
            });
            $scope.upload[index].then(function(response) {
                $timeout(function() {
                    //$scope.uploadResult.push(response.data);
                    //alert(response.data);
                    //$scope.nanoEntityForm = response.data;
                    $scope.saveFileData();
                    //$scope.loader = false;
                });
            }, function(response) {
                $timeout(function() {
                	//only for IE 9
                    if(navigator.appVersion.indexOf("MSIE 9.")!=-1) {
                        $scope.saveFileData();
                    }
                });
                if (response.status > 0) {
                	$scope.data.errors = response.status + ': ' + response.data;
                    $scope.loader = false;
                }
            }, function(evt) {
                // Math.min is to fix IE which reports 200% sometimes
                // $scope.progress[index] = Math.min(100, parseInt(100.0 * evt.loaded / evt.total));
            });
            $scope.upload[index].xhr(function(xhr){
//			xhr.upload.addEventListener('abort', function() {console.log('abort complete')}, false);
            });
        }
        else {
            $scope.saveFileData();
        }
    };

    $scope.saveFileData = function() {
        $scope.loader = true;

        var k=0;
        if ( $scope.fileForm.id != null && $scope.fileForm.id != '' ) {
            for (k = 0; k < $scope.currentFinding.files.length; ++k) {
                var element = $scope.currentFinding.files[k];
                if (element.id == $scope.fileForm.id) {
                    $scope.currentFinding.files[k] = $scope.fileForm;
                }
            }
        }
        /*            else {
         $scope.files.push($scope.fileForm);
         }
         */
        //$scope.currentFinding.files = $scope.files;

        if( $scope.currentFinding.theFile == null ) {
            $scope.currentFinding.theFile = {};
        }

        $scope.currentFinding.theFile.externalUrl = $scope.fileForm.externalUrl;
        if( $scope.selectedFileName != null && $scope.selectedFileName != '' ) {
        	$scope.currentFinding.theFile.uri = $scope.selectedFileName;
        } else {
        	$scope.currentFinding.theFile.uri = $scope.fileForm.uri;
        }
        $scope.currentFinding.theFile.uriExternal = $scope.fileForm.uriExternal;
        $scope.currentFinding.theFile.type = $scope.fileForm.type;
        $scope.currentFinding.theFile.title = $scope.fileForm.title;
        $scope.currentFinding.theFile.keywordsStr = $scope.fileForm.keywordsStr;
        $scope.currentFinding.theFile.description = $scope.fileForm.description;
        $scope.currentFinding.theFile.id = $scope.fileForm.id;
        $scope.currentFinding.theFile.theAccess = $scope.fileForm.theAccess;
        $scope.currentFinding.theFile.isPublic = $scope.fileForm.isPublic;
        $scope.currentFinding.theFile.createdBy = $scope.fileForm.createdBy;
        $scope.currentFinding.theFile.createdDate = $scope.fileForm.createdDate;

        if( $scope.fileForm.id == null || $scope.fileForm.id == '') {
            $scope.currentFinding.theFileIndex = -1;
        } else {
            $scope.currentFinding.theFileIndex = k-1;
        }

        if( $scope.sampleId != null ) {
            $scope.currentFinding.theFile.sampleId = $scope.sampleId;
        }

        $scope.messages = [];
        $http({method: 'POST', url: '/caNanoLab/rest/characterization/saveFile',data: $scope.currentFinding}).
            success(function(data, status, headers, config) {
                $scope.currentFinding = data;
                $scope.addNewFile = false;
                $scope.loader = false;
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
                // $rootScope.sampleData = data;
                $scope.loader = false;
                $scope.data.errors = data;
                $scope.addNewFile = true;
            });
    };

    $scope.getAddNewFile = function() {
        return $scope.addNewFile;
    };

    $scope.closeAddNewFile = function() {
        $scope.addNewFile = false;
    };

    $scope.columnInvalid = function() {
        for (var x = 0; x < $scope.currentFinding.columnHeaders.length;x++) {
            var col = $scope.currentFinding.columnHeaders[x];
            if (!col.columnType || col.columnType=='') {
                return 1
                break
            };
        };
    };

    $scope.disableColumn = function(option) {
        for (var x = 0; x < $scope.currentFinding.columnHeaders.length;x++) {
            var col = $scope.currentFinding.columnHeaders[x];
            if (col.columnName==option) {
                return 1;
                break;
            };

        };
    };

    $scope.openAddNewFile = function() {
        $scope.addNewFile = true;
        $scope.fileForm = {};

        $scope.fileForm.uriExternal = 'false';
        $scope.externalUrlEnabled = false;
    };

    /* End File Section */

 });
