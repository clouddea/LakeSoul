# SPDX-FileCopyrightText: 2023 LakeSoul Contributors
#
# SPDX-License-Identifier: Apache-2.0

# cython: language_level = 3

from libcpp.string cimport string
from libcpp.memory cimport shared_ptr
from pyarrow.includes.libarrow cimport CSchema

cdef extern from "lakesoul/lakesoul_dataset.h" namespace "lakesoul" nogil:
    cdef cppclass CLakeSoulDataset" lakesoul::LakeSoulDataset":
        CLakeSoulDataset(shared_ptr[CSchema])
        CLakeSoulDataset(shared_ptr[CSchema], CExpression)
        void AddFileUrl(string)

cdef extern from "lakesoul/lakesoul_fragment.h" namespace "lakesoul" nogil:
    cdef cppclass CLakeSoulFragment" lakesoul::LakeSoulFragment":
        pass
