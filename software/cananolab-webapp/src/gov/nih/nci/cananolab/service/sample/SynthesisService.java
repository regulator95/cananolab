package gov.nih.nci.cananolab.service.sample;

import gov.nih.nci.cananolab.domain.particle.Synthesis;
import gov.nih.nci.cananolab.domain.particle.SynthesisFunctionalization;
import gov.nih.nci.cananolab.domain.particle.SynthesisMaterial;
import gov.nih.nci.cananolab.domain.particle.SynthesisPurification;
import gov.nih.nci.cananolab.dto.particle.SampleBean;
import gov.nih.nci.cananolab.dto.particle.synthesis.SynthesisBean;
import gov.nih.nci.cananolab.dto.particle.synthesis.SynthesisFunctionalizationBean;
import gov.nih.nci.cananolab.dto.particle.synthesis.SynthesisMaterialBean;
import gov.nih.nci.cananolab.dto.particle.synthesis.SynthesisPurificationBean;
import gov.nih.nci.cananolab.exception.NoAccessException;
import gov.nih.nci.cananolab.exception.SynthesisException;
import gov.nih.nci.cananolab.service.BaseService;
import gov.nih.nci.cananolab.service.sample.helper.SynthesisHelper;

public interface SynthesisService extends BaseService {
    void copyAndSaveSynthesisFunctionalization(SynthesisFunctionalizationBean entityBean, SampleBean oldSampleBean,
                                               SampleBean[] newSampleBeans) throws SynthesisException,
            NoAccessException;
//TODO write

    void copyAndSaveSynthesisMaterial(SynthesisMaterialBean entityBean, SampleBean oldSampleBean,
                                      SampleBean[] newSampleBeans) throws SynthesisException, NoAccessException;

    void copyAndSaveSynthesisPurification(SynthesisPurificationBean entityBean, SampleBean oldSampleBean,
                                          SampleBean[] newSampleBeans) throws SynthesisException, NoAccessException;

    void deleteSynthesis(Synthesis synthesis) throws SynthesisException, NoAccessException;

    void deleteSynthesisFunctionalization(SynthesisFunctionalization synthesisFunctionalization) throws SynthesisException, NoAccessException;

    void deleteSynthesisMaterial(SynthesisMaterial synthesisMaterial) throws SynthesisException, NoAccessException;

    void deleteSynthesisPurification(SynthesisPurification synthesisPurification) throws SynthesisException,
            NoAccessException;

//    SynthesisBean findSynthesisById(Long sampleId, Long dataId) throws SynthesisException, NoAccessException;

    SynthesisBean findSynthesisBySampleId(Long sampleId) throws SynthesisException, NoAccessException;


    SynthesisFunctionalizationBean findSynthesisFunctionalizationById(Long sampleId, Long dataId) throws SynthesisException, NoAccessException;

    SynthesisMaterialBean findSynthesisMaterialById(Long sampleId, Long dataId) throws SynthesisException,
            NoAccessException;

    SynthesisPurificationBean findSynthesisPurificationById(Long sampleId, Long dataId) throws SynthesisException
            , NoAccessException;

    SynthesisHelper getHelper();

    void saveSynthesisFunctionalization(SampleBean sampleBean,
                                        SynthesisFunctionalizationBean synthesisFunctionalizationBean) throws SynthesisException, NoAccessException;

    void saveSynthesisMaterial(SampleBean sampleBean, SynthesisMaterialBean synthesisMaterialBean) throws SynthesisException, NoAccessException;

    void saveSynthesisPurification(SampleBean sampleBean, SynthesisPurificationBean synthesisPurificationBean) throws SynthesisException, NoAccessException;


}
