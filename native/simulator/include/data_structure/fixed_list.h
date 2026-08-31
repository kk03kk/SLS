//
// Created by gamerpuppy on 6/24/2021.
//

#ifndef STS_LIGHTSPEED_FIXEDLIST_H
#define STS_LIGHTSPEED_FIXEDLIST_H

#include <array>
#include <stdexcept>

namespace sts {

    template<typename T, int capacity>
    class fixed_list {
    private:
        int list_size = 0;
        std::array<T,capacity> arr;

    public:
        typedef T* iterator;
        typedef const T* const_iterator;

        fixed_list() = default;
        fixed_list(const fixed_list &rhs) = default;
        fixed_list &operator=(const fixed_list &rhs) = default;
        fixed_list(int size) : list_size(size) {
            if (size < 0 || size > capacity) throw std::length_error("fixed_list size out of range");
        }
//        fixed_list(fixed_list &&rhs)  noexcept : list_size(rhs.list_size), optionMap(std::move(rhs.optionMap)) {}

        fixed_list(std::initializer_list<T> l) {
            if (l.size() > static_cast<std::size_t>(capacity)) throw std::length_error("fixed_list initializer overflow");
            for (auto x : l) {
                arr[list_size++] = x;
            }
        }

        int size() const {
            return list_size;
        }

        iterator begin() {
            return arr.begin();
        }

        const_iterator begin() const {
            return arr.begin();
        }

        iterator end() {
            return arr.begin()+list_size;
        }

        const_iterator end() const {
            return arr.begin()+list_size;
        }

        T& operator[](int idx) {
            if (idx < 0 || idx >= list_size) throw std::out_of_range("fixed_list index");
            return arr[idx];
        }

        const T& operator[](int idx) const {
            if (idx < 0 || idx >= list_size) throw std::out_of_range("fixed_list index");
            return arr[idx];
        }

        T& front() {
            if (empty()) throw std::underflow_error("fixed_list is empty");
            return arr[0];
        }

        const T& front() const {
            if (empty()) throw std::underflow_error("fixed_list is empty");
            return arr[0];
        }

        T& back() {
            if (empty()) throw std::underflow_error("fixed_list is empty");
            return arr[list_size-1];
        }

        const T& back() const {
            if (empty()) throw std::underflow_error("fixed_list is empty");
            return arr[list_size-1];
        }

        T pop_back() {
            if (empty()) throw std::underflow_error("fixed_list is empty");
            return arr[--list_size];
        }


        void insert(int idx, T t) {
            if (list_size >= capacity) throw std::overflow_error("fixed_list overflow");
            if (idx < 0 || idx > list_size) throw std::out_of_range("fixed_list insert index");
            for (int i = list_size; i > idx; --i) {
                arr[i] = arr[i-1];
            }
            arr[idx] = t;
            list_size++;
        }

        void insert(iterator it, T t) {
            if (list_size >= capacity) throw std::overflow_error("fixed_list overflow");
            if (it < begin() || it > end()) throw std::out_of_range("fixed_list insert iterator");
            for (T* i = end(); i != it; --i) {
                *i = *(i-1);
            }
            *it = t;
            list_size++;
        }

        void push_back(T t) {
            if (list_size >= capacity) throw std::overflow_error("fixed_list overflow");
            arr[list_size++] = std::move(t);
        }

        void remove(int idx) {
            if (idx < 0 || idx >= list_size) throw std::out_of_range("fixed_list remove index");
            while (idx+1 < list_size) {
                arr[idx] = arr[idx+1];
                ++idx;
            }
            --list_size;
        }

        void erase(iterator it) {
            if (it < begin() || it >= end()) throw std::out_of_range("fixed_list erase iterator");
            while ((it+1) != end()) {
                *it = *(it+1);
                ++it;
            }
            --list_size;
        }

        void remove_back() {
            if (empty()) throw std::underflow_error("fixed_list is empty");
            --list_size;
        }

        [[nodiscard]] bool empty() const {
            return list_size == 0;
        }

        void clear() {
            list_size = 0;
        }

        void resize(int size) {
            if (size < 0 || size > capacity) throw std::length_error("fixed_list resize out of range");
            list_size = size;
        }

    };




}

#endif //STS_LIGHTSPEED_FIXEDLIST_H
